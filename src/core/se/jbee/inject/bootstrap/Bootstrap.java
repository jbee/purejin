/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Utils.arrayOf;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import se.jbee.inject.Injector;
import se.jbee.inject.Utils;
import se.jbee.inject.config.ConstructsBy;
import se.jbee.inject.config.Edition;
import se.jbee.inject.config.Env;
import se.jbee.inject.config.Environment;
import se.jbee.inject.config.Globals;
import se.jbee.inject.config.HintsBy;
import se.jbee.inject.config.NamesBy;
import se.jbee.inject.config.ProducesBy;
import se.jbee.inject.config.ScopesBy;
import se.jbee.inject.container.Container;
import se.jbee.inject.container.Lazy;
import se.jbee.inject.declare.Bundle;
import se.jbee.inject.declare.Module;

/**
 * Utility to create an {@link Injector} context from {@link Bundle}s and
 * {@link Module}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Bootstrap {

	public static final Environment ENV = new Environment() //
			.with(Edition.class, Edition.FULL) //
			.withMacro(Macros.EXPAND) //
			.withMacro(Macros.NEW) //
			.withMacro(Macros.CONSTANT) //
			.withMacro(Macros.PRODUCES) //
			.withMacro(Macros.INSTANCE_REF) //
			.withMacro(Macros.PARAMETRIZED_REF) //
			.withMacro(Macros.ARRAY) //
			.with(ConstructsBy.class, ConstructsBy.common) //
			.with(ProducesBy.class, ProducesBy.noMethods) //
			.with(NamesBy.class, NamesBy.defaultName) //
			.with(ScopesBy.class, ScopesBy.alwaysDefault) //
			.with(HintsBy.class, HintsBy.noParameters) //
			.readonly();

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

	@SuppressWarnings("unchecked")
	private static Injector loadApplicationContext() {
		Set<Class<? extends Bundle>> roots = new LinkedHashSet<>();
		for (Bundle root : ServiceLoader.load(Bundle.class))
			roots.add(root.getClass());
		if (roots.isEmpty())
			throw InconsistentBinding.noRootBundle();
		Env env = null; //FIXME
		return injector(Bindings.newBindings(), env,
				roots.toArray(new Class[0]));
	}

	@SafeVarargs
	public static Injector injector(Class<? extends Bundle>... roots) {
		return injector(Bindings.newBindings(), ENV, roots);
	}

	@SafeVarargs
	public static Injector injector(Bindings bindings, Env env,
			Class<? extends Bundle>... roots) {
		BuildinBootstrapper bootstrapper = new BuildinBootstrapper(env);
		Class<? extends Bundle>[] bundles = bootstrapper.bundleAll(roots);
		return injector(env, bindings, bootstrapper.modulesOf(bundles));
	}

	public static Injector injector(Class<? extends Bundle> root) {
		return injector(root, ENV);
	}

	public static Injector injector(Class<? extends Bundle> root, Env env) {
		return injector(root, Bindings.newBindings(), env);
	}

	public static Injector injector(Class<? extends Bundle> root,
			Bindings bindings, Env env) {
		return injector(env, bindings, modulariser(env).modularise(root));
	}

	// TODO move env to be the first param everywhere

	public static Injector injector(Env env, Bindings bindings,
			Module[] modules) {
		return Container.injector(
				Binding.disambiguate(bindings.declaredFrom(env, modules)));
	}

	public static Modulariser modulariser(Env env) {
		return new BuildinBootstrapper(env);
	}

	public static Bundler bundler(Env env) {
		return new BuildinBootstrapper(env);
	}

	public static Binding<?>[] bindings(Class<? extends Bundle> root) {
		return bindings(root, Bindings.newBindings(), ENV);
	}

	public static Binding<?>[] bindings(Class<? extends Bundle> root,
			Bindings bindings, Env env) {
		return Binding.disambiguate(bindings//
				.declaredFrom(env, modulariser(env).modularise(root)));
	}

	public static void nonnullThrowsReentranceException(Object field) {
		if (field != null)
			throw InconsistentBinding.contextAlreadyInitialised();
	}

	private Bootstrap() {
		throw new UnsupportedOperationException("util");
	}

	private static final class BuildinBootstrapper
			implements Bootstrapper, Bundler, Modulariser {

		private final Map<Class<? extends Bundle>, Set<Class<? extends Bundle>>> bundleChildren = new IdentityHashMap<>();
		private final Map<Class<? extends Bundle>, List<Module>> bundleModules = new IdentityHashMap<>();
		private final Set<Class<? extends Bundle>> uninstalled = new HashSet<>();
		private final Set<Class<? extends Bundle>> installed = new HashSet<>();
		private final LinkedList<Class<? extends Bundle>> stack = new LinkedList<>();
		private final Env env;
		private final Edition edition;

		BuildinBootstrapper(Env env) {
			this.env = env;
			this.edition = env.property(Edition.class, Env.class.getPackage());
		}

		@Override
		public void install(Class<? extends Bundle> bundle) {
			if (uninstalled.contains(bundle) || installed.contains(bundle))
				return;
			if (!edition.featured(bundle)) {
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
			Bundle instance = Utils.instance(bundle);
			instance.bootstrap(this);
			if (stack.pop() != bundle)
				throw new IllegalStateException(bundle.getCanonicalName());
		}

		@Override
		public <F extends Enum<F>> void install(
				Class<? extends ToggledBundles<F>> bundle,
				final Class<F> flags) {
			if (!edition.featured(bundle))
				return;
			Utils.instance(bundle).bootstrap((bundleForFlag, flag) -> {
				// NB: null is a valid value to define what happens when no configuration is present
				if (env.toggled(flags, flag, bundleForFlag.getPackage())) {
					BuildinBootstrapper.this.install(bundleForFlag);
				}
			});
		}

		@Override
		@SafeVarargs
		public final <F extends Enum<F> & ToggledBundles<F>> void install(
				F... flags) {
			if (flags.length > 0) {
				final F flag0 = flags[0];
				if (!edition.featured(flag0.getClass()))
					return;
				final EnumSet<F> installing = EnumSet.of(flag0, flags);
				flag0.bootstrap((bundle, flag) -> {
					if (installing.contains(flag))
						BuildinBootstrapper.this.install(bundle);
				});
			}
		}

		@Override
		public void install(Module module) {
			Class<? extends Bundle> bundle = stack.peek();
			if (uninstalled.contains(bundle)
				|| !edition.featured(module.getClass()))
				return;
			bundleModules.computeIfAbsent(bundle, key -> new ArrayList<>()).add(
					module);
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
		public final <F extends Enum<F> & ToggledBundles<F>> void uninstall(
				F... flags) {
			if (flags.length > 0) {
				final EnumSet<F> uninstalling = EnumSet.of(flags[0], flags);
				flags[0].bootstrap((bundle, flag) -> {
					if (uninstalling.contains(flag))
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
