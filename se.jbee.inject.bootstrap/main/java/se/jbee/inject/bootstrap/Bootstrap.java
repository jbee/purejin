/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.Env;
import se.jbee.inject.InconsistentDeclaration;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.Module;
import se.jbee.inject.bind.*;
import se.jbee.inject.binder.ServiceLoaderAnnotations;
import se.jbee.inject.binder.ServiceLoaderBundles;
import se.jbee.inject.binder.ServiceLoaderEnvBundles;
import se.jbee.inject.config.Edition;
import se.jbee.inject.container.Container;
import se.jbee.inject.defaults.DefaultEnv;
import se.jbee.inject.defaults.DefaultsBundle;
import se.jbee.inject.lang.Reflect;

import java.lang.reflect.Modifier;
import java.util.*;

import static se.jbee.inject.bind.Bindings.newBindings;
import static se.jbee.inject.lang.Utils.arrayOf;
import static se.jbee.inject.lang.Utils.arrayPrepend;

/**
 * Utility to create an {@link Injector} or {@link Env} context from {@link
 * Bundle}s and {@link Module}s.
 */
public final class Bootstrap {

	public static final Env DEFAULT_ENV = DefaultEnv.bootstrap();

	/**
	 * @return The {@link Injector} context purely created from {@link Bundle}s
	 * found using {@link java.util.ServiceLoader}.
	 */
	public static Injector injector() {
		return injector(env());
	}

	public static Injector injector(Env env) {
		return injector(env, ServiceLoaderBundles.class);
	}

	/**
	 * @return The {@link Env} context purely created from {@link Bundle}s found
	 * using {@link ServiceLoader}. These need to have the {@link
	 * se.jbee.inject.Extends} annotation referring to the {@link Env} class.
	 */
	public static Env env() {
		return env(DEFAULT_ENV);
	}

	public static Env env(Env env) {
		return env(env, ServiceLoaderEnvBundles.class, ServiceLoaderAnnotations.class);
	}

	public static Env env(Class<? extends Bundle> root) {
		return env(DEFAULT_ENV, root);
	}

	@SafeVarargs
	public static Env env(Class<? extends Bundle>... roots) {
		return env(DEFAULT_ENV, roots);
	}

	public static Env env(Env env, Class<? extends Bundle> root) {
		return injector(env, newBindings(), root, DefaultEnv.class).asEnv();
	}

	@SafeVarargs
	public static Env env(Env env, Class<? extends Bundle>... roots) {
		return injector(env, newBindings(), arrayPrepend(DefaultEnv.class, roots)).asEnv();
	}

	@SafeVarargs
	public static Injector injector(Env env, Bindings bindings,
			Class<? extends Bundle>... roots) {
		BuiltinBootstrapper boots = new BuiltinBootstrapper(env);
		return injector(env, bindings, boots.modulesOf(boots.bundleAll(roots)));
	}

	public static Injector injector(Class<? extends Bundle> root) {
		return injector(DEFAULT_ENV, root);
	}

	public static Injector injector(Env env, Class<? extends Bundle> root) {
		return injector(env, root, newBindings());
	}

	public static Injector injector(Env env, Class<? extends Bundle> root,
			Bindings bindings) {
		return injector(env, bindings, modulariser(env).modularise(root));
	}

	private static Injector injector(Env env, Bindings bindings,
			Module[] modules) {
		return Container.injector(
				env.property(BindingConsolidation.class) //
						.consolidate(env,
								(bindings.declaredFrom(env, modules))));
	}

	public static Modulariser modulariser(Env env) {
		return new BuiltinBootstrapper(env);
	}

	public static Bundler bundler(Env env) {
		return new BuiltinBootstrapper(env);
	}

	private Bootstrap() {
		throw new UnsupportedOperationException("util");
	}

	private static final class BuiltinBootstrapper
			implements Bootstrapper, Bundler, Modulariser {

		private final Map<Class<? extends Bundle>, Set<Class<? extends Bundle>>> bundleChildren = new IdentityHashMap<>();
		private final Map<Class<? extends Bundle>, List<Module>> bundleModules = new IdentityHashMap<>();
		private final Set<Class<? extends Bundle>> uninstalled = new HashSet<>();
		private final Set<Class<? extends Bundle>> installed = new HashSet<>();
		private final LinkedList<Class<? extends Bundle>> stack = new LinkedList<>();
		private final Env env;
		private final Edition edition;

		BuiltinBootstrapper(Env env) {
			this.env = env;
			this.edition = env.property(Edition.class, Edition.FULL);
		}

		@Override
		public void installDefaults() {
			install(DefaultsBundle.class);
			install(InjectorFeature.SUB_CONTEXT_FUNCTION);
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
			Bundle instance = createBundle(bundle);
			instance.bootstrap(this);
			if (stack.pop() != bundle)
				throw new IllegalStateException(bundle.getCanonicalName());
		}

		private <T> T createBundle(Class<T> bundle) {
			// OBS: Here we do not use the env but always make the bundles accessible
			// as this is kind of designed into the concept
			return Reflect.construct(bundle, c-> {
					if (!Modifier.isPublic(c.getModifiers()))
						Reflect.accessible(c);
					},
					e -> new InconsistentDeclaration("Failed to create bundle: " + bundle, e));
		}

		@Override
		public <F extends Enum<F>> void install(
				Class<? extends Dependent<F>> bundle, final Class<F> dependentOn) {
			if (!edition.featured(bundle))
				return;
			createBundle(bundle).bootstrap((actual, bundleForFlag) -> {
				// NB: null is a valid value to define what happens when no configuration is present
				if (env.isInstalled(dependentOn, actual)) {
					BuiltinBootstrapper.this.install(bundleForFlag);
				}
			});
		}

		@Override
		@SafeVarargs
		public final <F extends Enum<F> & Dependent<F>> void install(F... elements) {
			if (elements.length > 0) {
				final F flag0 = elements[0];
				if (!edition.featured(flag0.getClass()))
					return;
				final EnumSet<F> installing = EnumSet.of(flag0, elements);
				flag0.bootstrap((on, bundle) -> {
					if (installing.contains(on))
						BuiltinBootstrapper.this.install(bundle);
				});
			}
		}

		@Override
		@SafeVarargs
		public final <F extends Enum<F> & Dependent<F>> void uninstall(
				F... elements) {
			if (elements.length > 0) {
				final EnumSet<F> uninstalling = EnumSet.of(elements[0],
						elements);
				elements[0].bootstrap((on, bundle) -> {
					if (uninstalling.contains(on))
						uninstall(bundle);
				});
			}
		}

		@Override
		public void install(Module module) {
			Class<? extends Bundle> bundle = stack.peek();
			if (uninstalled.contains(bundle)
				|| !edition.featured(module.getClass()))
				return;
			bundleModules.computeIfAbsent(bundle, //
					key -> new ArrayList<>()).add(module);
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
			Set<Class<? extends Bundle>> newlyInstalled = new LinkedHashSet<>();
			for (Class<? extends Bundle> root : roots)
				if (!newlyInstalled.contains(root)) {
					install(root);
					newlyInstalled.add(root);
				}
			for (Class<? extends Bundle> root : roots)
				addAllInstalledIn(root, newlyInstalled);
			return arrayOf(newlyInstalled, Class.class);
		}

		final Module[] modulesOf(Class<? extends Bundle>[] bundles) {
			List<Module> newlyInstalled = new ArrayList<>(bundles.length);
			for (Class<? extends Bundle> b : bundles) {
				List<Module> modules = bundleModules.get(b);
				if (modules != null)
					newlyInstalled.addAll(modules);
			}
			return arrayOf(newlyInstalled, Module.class);
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
