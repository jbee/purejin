/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.accessible;
import static se.jbee.inject.Utils.arrayOf;
import static se.jbee.inject.Utils.noArgsConstructor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.config.Choices;
import se.jbee.inject.config.Globals;
import se.jbee.inject.config.Options;
import se.jbee.inject.container.Container;
import se.jbee.inject.container.Lazy;

/**
 * Utility to create an {@link Injector} context from {@link Bundle}s and
 * {@link Module}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Bootstrap {

	private static final Lazy<Injector> APPLICATION_CONTEXT = new Lazy<>();

	/**
	 * The <em>Application Context</em> is a implicitly defined {@link Injector}
	 * context usually used by applications assembled from multiple software
	 * modules.
	 * 
	 * Instead of passing a single root {@link Bundle} to the bootstrapping one
	 * or more root {@link Bundle}s are defined as services for the
	 * {@link ServiceLoader} in the
	 * <code>META-INF/services/se.jbee.inject.bootstrap.Bundle</code> files or
	 * jar or war files. As usual such a file contains only the fully qualified
	 * class name of the root bundle for the software module the jar represents.
	 * As an application can consist of multiple software modules there can be
	 * multiple root bundles. The application root bundle can be seen as a
	 * virtual bundle installing all the bundles referenced by
	 * {@link ServiceLoader}.
	 * 
	 * Instead of passing configuration like {@link Globals} to this method
	 * these can be configured by implementing the
	 * {@link ApplicationContextConfig} interface and declaring it as a service
	 * in
	 * <code>META-INF/services/se.jbee.inject.bootstrap.ApplicationContextConfig</code>
	 * of one of the application jars so it can be loaded via
	 * {@link ServiceLoader}. This allows application specific configuration of
	 * the bootstrapped application context.
	 * 
	 * Once created the {@link Injector} instance is cached so further calls to
	 * this method always return the same instance.
	 * 
	 * @since 19.1
	 * 
	 * @return the {@link Injector} instance derived from one or more roots
	 *         defined using the {@link ServiceLoader} mechanism
	 */
	public static Injector getApplicationContext() {
		return APPLICATION_CONTEXT.get(Bootstrap::loadApplicationContext);
	}

	private static Injector loadApplicationContext() {
		Set<Class<? extends Bundle>> roots = new LinkedHashSet<>();
		for (Bundle root : ServiceLoader.load(Bundle.class))
			roots.add(root.getClass());
		if (roots.isEmpty())
			throw InconsistentBinding.noRootBundle();
		Iterator<ApplicationContextConfig> configIter = ServiceLoader.load(
				ApplicationContextConfig.class).iterator();
		Globals globals = Globals.STANDARD;
		Bindings bindings = Bindings.newBindings();
		if (configIter.hasNext()) {
			ApplicationContextConfig config = configIter.next();
			globals = config.globals();
			bindings = bindings.with(config.macros()).with(config.mirrors());
		}
		BuildinBootstrapper bootstrapper = new BuildinBootstrapper(globals);
		@SuppressWarnings("unchecked")
		Class<? extends Bundle>[] bundles = bootstrapper.bundleAll(
				roots.toArray(new Class[0]));
		return injector(bindings, bootstrapper.modulesOf(bundles));
	}

	public static Injector injector(Class<? extends Bundle> root) {
		return injector(root, Globals.STANDARD);
	}

	public static Injector injector(Class<? extends Bundle> root,
			Globals globals) {
		return injector(root, Bindings.newBindings(), globals);
	}

	public static Injector injector(Class<? extends Bundle> root,
			Bindings bindings, Globals globals) {
		return injector(bindings, modulariser(globals).modularise(root));
	}

	public static Injector injector(Bindings bindings, Module[] modules) {
		return Container.injector(
				Binding.disambiguate(bindings.declareFrom(modules)));
	}

	public static Modulariser modulariser(Globals globals) {
		return new BuildinBootstrapper(globals);
	}

	public static Bundler bundler(Globals globals) {
		return new BuildinBootstrapper(globals);
	}

	public static Binding<?>[] bindings(Class<? extends Bundle> root,
			Bindings bindings, Globals globals) {
		return Binding.disambiguate(
				bindings.declareFrom(modulariser(globals).modularise(root)));
	}

	public static <T> Module module(ModuleWith<T> module, Options presets) {
		return new PresetModuleBridge<>(module, presets);
	}

	public static void nonnullThrowsReentranceException(Object field) {
		if (field != null)
			throw InconsistentBinding.contextAlreadyInitialised();
	}

	public static <T> T instance(Class<T> type) {
		return Supply.construct(accessible(noArgsConstructor(type)));
	}

	private Bootstrap() {
		throw new UnsupportedOperationException("util");
	}

	/**
	 * Implements the {@link ModuleWith} abstraction by presenting them as
	 * {@link Module}.
	 * 
	 * @param <T> type of the {@link Options} value injected into the
	 *            {@link ModuleWith}
	 */
	private static final class PresetModuleBridge<T> implements Module {

		private final ModuleWith<T> module;
		private final Options presets;

		PresetModuleBridge(ModuleWith<T> module, Options presets) {
			this.module = module;
			this.presets = presets;
		}

		@Override
		public void declare(Bindings bindings) {
			Type<?> valueType = Type.supertype(ModuleWith.class,
					raw(module.getClass())).parameter(0);
			@SuppressWarnings("unchecked")
			final T value = (T) (valueType.rawType == Options.class
				? presets
				: presets.get(valueType));
			module.declare(bindings, value);
		}
	}

	private static final class BuildinBootstrapper
			implements Bootstrapper, Bundler, Modulariser {

		private final Map<Class<? extends Bundle>, Set<Class<? extends Bundle>>> bundleChildren = new IdentityHashMap<>();
		private final Map<Class<? extends Bundle>, List<Module>> bundleModules = new IdentityHashMap<>();
		private final Set<Class<? extends Bundle>> uninstalled = new HashSet<>();
		private final Set<Class<? extends Bundle>> installed = new HashSet<>();
		private final LinkedList<Class<? extends Bundle>> stack = new LinkedList<>();
		private final Globals globals;

		BuildinBootstrapper(Globals globals) {
			this.globals = globals;
		}

		@Override
		public void install(Class<? extends Bundle> bundle) {
			if (uninstalled.contains(bundle) || installed.contains(bundle))
				return;
			if (!globals.edition.featured(bundle)) {
				// this way we will never ask again - something not featured is finally not featured
				uninstalled.add(bundle);
				return;
			}
			installed.add(bundle);
			if (!stack.isEmpty()) {
				final Class<? extends Bundle> parent = stack.peek();
				bundleChildren.computeIfAbsent(parent,
						key -> new LinkedHashSet<>()).add(bundle);
			}
			stack.push(bundle);
			Bootstrap.instance(bundle).bootstrap(this);
			if (stack.pop() != bundle)
				throw new IllegalStateException(bundle.getCanonicalName());
		}

		@Override
		public <C extends Enum<C>> void install(
				Class<? extends ChoiceBundle<C>> bundle,
				final Class<C> property) {
			if (!globals.edition.featured(property))
				return;
			final Choices choices = globals.choices;
			Bootstrap.instance(bundle).bootstrap((bundleForChoice, choice) -> {
				// NB: null is a valid value to define what happens when no configuration is present
				if (choices.isChosen(property, choice)) {
					BuildinBootstrapper.this.install(bundleForChoice);
				}
			});
		}

		@Override
		@SafeVarargs
		public final <C extends Enum<C> & ChoiceBundle<C>> void install(
				C... choices) {
			if (choices.length > 0) {
				final C choice0 = choices[0];
				if (!globals.edition.featured(choice0.getClass()))
					return;
				final EnumSet<C> installing = EnumSet.of(choice0, choices);
				choice0.bootstrap((bundle, onOption) -> {
					if (installing.contains(onOption))
						BuildinBootstrapper.this.install(bundle);
				});
			}
		}

		@Override
		public void install(Module module) {
			Class<? extends Bundle> bundle = stack.peek();
			if (uninstalled.contains(bundle)
				|| !globals.edition.featured(module.getClass()))
				return;
			bundleModules.computeIfAbsent(bundle, key -> new ArrayList<>()).add(
					module);
		}

		@Override
		public <T> void install(ModuleWith<T> module) {
			install(module(module, globals.options));
		}

		@Override
		public Module[] modularise(Class<? extends Bundle> root) {
			return modulesOf(bundle(root));
		}

		@Override
		public Class<? extends Bundle>[] bundle(Class<? extends Bundle> root) {
			return bundleAll(root);
		}

		@SafeVarargs
		@SuppressWarnings("unchecked")
		final Class<? extends Bundle>[] bundleAll(
				Class<? extends Bundle>... roots) {
			Set<Class<? extends Bundle>> installed = new LinkedHashSet<>();
			for (Class<? extends Bundle> root : roots)
				if (!installed.contains(root))
					install(root);
			for (Class<? extends Bundle> root : roots)
				addAllInstalledIn(root, installed);
			return arrayOf(installed, Class.class);
		}

		final Module[] modulesOf(Class<? extends Bundle>[] bundles) {
			List<Module> installed = new ArrayList<>(bundles.length);
			for (Class<? extends Bundle> b : bundles) {
				List<Module> modules = bundleModules.get(b);
				if (modules != null)
					installed.addAll(modules);
			}
			return arrayOf(installed, Module.class);
		}

		@Override
		public void uninstall(Class<? extends Bundle> bundle) {
			if (uninstalled.contains(bundle))
				return;
			uninstalled.add(bundle);
			installed.remove(bundle);
			for (Set<Class<? extends Bundle>> c : bundleChildren.values())
				c.remove(bundle);
			bundleModules.remove(bundle); // we are sure we don't need its modules
		}

		@Override
		@SafeVarargs
		public final <O extends Enum<O> & ChoiceBundle<O>> void uninstall(
				O... bundles) {
			if (bundles.length > 0) {
				final EnumSet<O> uninstalling = EnumSet.of(bundles[0], bundles);
				bundles[0].bootstrap((bundle, choice) -> {
					if (uninstalling.contains(choice))
						uninstall(bundle);
				});
			}
		}

		private void addAllInstalledIn(Class<? extends Bundle> bundle,
				Set<Class<? extends Bundle>> accu) {
			accu.add(bundle);
			Set<Class<? extends Bundle>> children = bundleChildren.get(bundle);
			if (children == null)
				return;
			for (Class<? extends Bundle> c : children)
				if (!accu.contains(c))
					addAllInstalledIn(c, accu);
		}

	}
}
