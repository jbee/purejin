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
import se.jbee.inject.config.New;
import se.jbee.inject.container.Container;
import se.jbee.inject.defaults.DefaultEnv;
import se.jbee.inject.defaults.DefaultsBundle;
import se.jbee.lang.Lazy;

import java.util.*;

import static se.jbee.inject.bind.Bindings.newBindings;
import static se.jbee.lang.Utils.arrayOf;
import static se.jbee.lang.Utils.arrayPrepend;

/**
 * Utility to create an {@link Injector} or {@link Env} context from {@link
 * Bundle}s and {@link Module}s.
 */
public final class Bootstrap {

	public static final Env DEFAULT_ENV = DefaultEnv.bootstrap();

	private static final Lazy<Env> SERVICE_LOADER_ENV = new Lazy<>();
	private static final Lazy<Injector> SERVICE_LOADER_INJECTOR = new Lazy<>();

	/**
	 * @return The {@link Env} purely defined by {@link Bundle}s provided via
	 * {@link ServiceLoader}. Once created this method always returns the same
	 * {@link Env} instance.
	 */
	public static Env currentEnv() {
		return SERVICE_LOADER_ENV.get(Bootstrap::env);
	}

	/**
	 * @return The {@link Injector} purely defined by {@link Bundle}s provided
	 * via {@link ServiceLoader}. The bootstrapping always uses the {@link
	 * #currentEnv()}. Once created this method always returns the same {@link
	 * Injector} instance.
	 */
	public static Injector currentInjector() {
		return SERVICE_LOADER_INJECTOR.get(() -> injector(currentEnv()));
	}

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
		return injector(env, bindings, modules(env).installedModules(root));
	}

	private static Injector injector(Env env, Bindings bindings,
			Module[] modules) {
		return Container.injector(
				env.property(BindingConsolidation.class) //
						.consolidate(env,
								(bindings.declaredFrom(env, modules))));
	}

	public static ModuleBootstrapper modules(Env env) {
		return new BuiltinBootstrapper(env);
	}

	public static BundleBootstrapper bundles(Env env) {
		return new BuiltinBootstrapper(env);
	}

	private Bootstrap() {
		throw new UnsupportedOperationException("util");
	}

	private static final class BuiltinBootstrapper
			implements Bootstrapper, BundleBootstrapper, ModuleBootstrapper {

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
			createBundle(bundle).bootstrap(this);
			if (stack.pop() != bundle)
				throw new IllegalStateException(bundle.getCanonicalName());
		}

		private <T> T createBundle(Class<T> bundle) {
			// OBS: Here we do not use the env but always make the bundles accessible
			// as this is kind of designed into the concept
			try {
				New newBundle = env.in(bundle).property(New.class);
				return newBundle.call(bundle.getDeclaredConstructor(), new Object[0]);
			} catch (Exception e) {
				throw new InconsistentDeclaration(
						"Failed to create bundle: " + bundle, e);
			}
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
				final F e0 = elements[0];
				if (!edition.featured(e0.getClass()))
					return;
				final EnumSet<F> installing = EnumSet.of(e0, elements);
				e0.bootstrap((on, bundle) -> {
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
		public Module[] installedModules(Class<? extends Bundle> root) {
			return modulesOf(installedBundles(root));
		}

		@Override
		public Class<? extends Bundle>[] installedBundles(Class<? extends Bundle> root) {
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
