/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.lang.Utils.arrayOf;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.jbee.inject.*;
import se.jbee.inject.bind.Binding;
import se.jbee.inject.bind.Bindings;
import se.jbee.inject.bind.Bootstrapper;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.Bundler;
import se.jbee.inject.bind.Modulariser;
import se.jbee.inject.bind.Module;
import se.jbee.inject.bind.Toggled;
import se.jbee.inject.config.Edition;
import se.jbee.inject.container.Container;
import se.jbee.inject.defaults.DefaultsBundle;
import se.jbee.inject.lang.Utils;

/**
 * Utility to create an {@link Injector} context from {@link Bundle}s and
 * {@link Module}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Bootstrap {

	public static Env env(Class<? extends Bundle> root) {
		return Environment.DEFAULT.complete(injector(root).asEnv());
	}

	public static Env env(Env env, Class<? extends Bundle> root) {
		return Environment.DEFAULT.complete(injector(env, root).asEnv());
	}

	@SafeVarargs
	public static Injector injector(Env env, Bindings bindings,
			Class<? extends Bundle>... roots) {
		BuiltinBootstrapper boots = new BuiltinBootstrapper(env);
		return injector(env, bindings, boots.modulesOf(boots.bundleAll(roots)));
	}

	public static Injector injector(Class<? extends Bundle> root) {
		return injector(Environment.DEFAULT, root);
	}

	public static Injector injector(Env env, Class<? extends Bundle> root) {
		return injector(env, root, Bindings.newBindings());
	}

	public static Injector injector(Env env, Class<? extends Bundle> root,
			Bindings bindings) {
		return injector(env, bindings, modulariser(env).modularise(root));
	}

	public static Injector injector(Env env, Bindings bindings,
			Module[] modules) {
		return Container.injector(
				Binding.disambiguate(bindings.declaredFrom(env, modules)));
	}

	public static Modulariser modulariser(Env env) {
		return new BuiltinBootstrapper(env);
	}

	public static Bundler bundler(Env env) {
		return new BuiltinBootstrapper(env);
	}

	public static Binding<?>[] bindings(Env env, Class<? extends Bundle> root,
			Bindings bindings) {
		return Binding.disambiguate(bindings//
				.declaredFrom(env, modulariser(env).modularise(root)));
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
			this.edition = env.property(Edition.class, Env.class.getPackage());
		}

		@Override
		public void installDefaults() {
			install(DefaultsBundle.class);
			install(InjectorFeature.SUB_CONTEXT_FUNCTION,
					InjectorFeature.INSTANCE_RESOLVE_FUNCTION);
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
			return Utils.instantiate(bundle, Utils::accessible,
					e -> new InconsistentDeclaration("Failed to create bundle: " + bundle, e));
		}

		@Override
		public <F extends Enum<F>> void install(
				Class<? extends Toggled<F>> bundle, final Class<F> flags) {
			if (!edition.featured(bundle))
				return;
			createBundle(bundle).bootstrap((bundleForFlag, flag) -> {
				// NB: null is a valid value to define what happens when no configuration is present
				if (env.toggled(flags, flag, bundleForFlag.getPackage())) {
					BuiltinBootstrapper.this.install(bundleForFlag);
				}
			});
		}

		@Override
		@SafeVarargs
		public final <F extends Enum<F> & Toggled<F>> void install(F... flags) {
			if (flags.length > 0) {
				final F flag0 = flags[0];
				if (!edition.featured(flag0.getClass()))
					return;
				final EnumSet<F> installing = EnumSet.of(flag0, flags);
				flag0.bootstrap((bundle, flag) -> {
					if (installing.contains(flag))
						BuiltinBootstrapper.this.install(bundle);
				});
			}
		}

		@Override
		@SafeVarargs
		public final <F extends Enum<F> & Toggled<F>> void uninstall(
				F... flags) {
			if (flags.length > 0) {
				final EnumSet<F> uninstalling = EnumSet.of(flags[0], flags);
				flags[0].bootstrap((bundle, flag) -> {
					if (uninstalling.contains(flag))
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
			Set<Class<? extends Bundle>> newlyInstalled = new LinkedHashSet<>();
			for (Class<? extends Bundle> root : roots)
				if (!newlyInstalled.contains(root))
					install(root);
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
