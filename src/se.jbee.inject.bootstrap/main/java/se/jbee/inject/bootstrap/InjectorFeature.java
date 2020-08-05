package se.jbee.inject.bootstrap;

import se.jbee.inject.*;
import se.jbee.inject.bind.Bindings;
import se.jbee.inject.bind.Bootstrapper;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.Toggled;
import se.jbee.inject.binder.BinderModule;

import java.util.function.Function;

import static se.jbee.inject.Cast.functionTypeOf;

/**
 * Capabilities of the basic {@link Injector} that can be installed or uninstalled.
 *
 * By default the {@link Bootstrap} utility will install all features.
 * Use {@link Bootstrapper#uninstall(Enum[])} to remove any unwanted capability.
 */
public enum InjectorFeature implements Toggled<InjectorFeature> {
	/**
	 * Binds a {@link Function} that given an array of {@link Class} (that must
	 * extend {@link Bundle}) creates or returns an {@link Injector} sub-context
	 * bootstrapped from the given set of classes.
	 */
	SUB_CONTEXT_FUNCTION,

	/**
	 * Binds a {@link Function} that given an {@link Instance} resolves it in
	 * the {@link Injector} context. This is just an indirect way to call {@link
	 * Injector#resolve(Instance)} without becoming directly dependent on the
	 * {@link Injector} abstraction.
	 */
	INSTANCE_RESOLVE_FUNCTION,
	;

	@Override
	public void bootstrap(Bootstrapper.Toggler<InjectorFeature> bootstrapper) {
		bootstrapper.install(SubContextFunction.class, SUB_CONTEXT_FUNCTION);
	}

	static final class SubContextFunction extends BinderModule {

		@Override
		protected void declare() {
			Env env = env();
			asDefault().bind(functionTypeOf(Class[].class, Injector.class)) //
					.to(roots -> createSubContextFromRootBundles(env, roots));
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private Injector createSubContextFromRootBundles(Env env, Class[] roots) {
			return Bootstrap.injector(env, Bindings.newBindings(), roots);
		}
	}

	static final class ResolveInstanceFunction extends BinderModule {

		@Override
		protected void declare() {
			asDefault().bind(functionTypeOf(Instance.class, Object.class)) //
					.toSupplier(ResolveInstanceFunction::resolveInstance);
		}

		@SuppressWarnings("rawtypes")
		private static Function<Instance, Object> resolveInstance(
				Dependency<? super Function<Instance, Object>> dep,
				Injector context) {
			return context::resolve;
		}
	}
}
