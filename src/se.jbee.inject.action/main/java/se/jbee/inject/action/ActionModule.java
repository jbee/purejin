/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.action;

import static java.util.Arrays.asList;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Type.parameterTypes;
import static se.jbee.inject.lang.Type.raw;
import static se.jbee.inject.lang.Type.returnType;
import static se.jbee.inject.lang.Utils.arrayFindFirst;
import static se.jbee.inject.config.ProducesBy.allMethods;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.jbee.inject.Dependency;
import se.jbee.inject.Hint;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Scope;
import se.jbee.inject.Supplier;
import se.jbee.inject.lang.Type;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.UnresolvableDependency.SupplyFailed;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Utils;
import se.jbee.inject.bind.Module;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.config.Plugins;
import se.jbee.inject.config.ProducesBy;
import se.jbee.inject.InjectionSite;

/**
 * When binding {@link Action}s this {@link Module} can be extended.
 *
 * It provides procedure-related bind methods.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class ActionModule extends BinderModule {

	/**
	 * The {@link ProducesBy} picks the {@link Method}s that are used to
	 * implement {@link Action}s. This abstraction allows to customise what
	 * methods are bound as {@link Action}s. The
	 * {@link ProducesBy#reflect(Class)} should return all methods in the
	 * given {@link Class} that should be used to implement an {@link Action}.
	 */
	static final Instance<ProducesBy> ACTION_MIRROR = instance(
			named(Action.class), raw(ProducesBy.class));

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <I, O> Dependency<Action<I, O>> actionDependency(
			Type<I> input, Type<O> output) {
		Type type = raw(Action.class).parametized(input, output);
		return dependency(type);
	}

	protected final void bindActionsIn(Class<?> impl) {
		plug(impl).into(Action.class);
	}

	protected final void discoverActionsBy(ProducesBy mirror) {
		bind(ACTION_MIRROR).to(mirror);
	}

	protected ActionModule() {
		super(ActionBaseModule.class);
	}

	private static final class ActionBaseModule extends BinderModule {

		@Override
		public void declare() {
			asDefault().per(Scope.dependencyType).starbind(
					Action.class).toSupplier(ActionSupplier.class);
			asDefault().per(Scope.application).bind(ACTION_MIRROR).to(
					allMethods);
			asDefault().per(Scope.application).bind(Executor.class).to(
					DirectExecutor.class);
		}

	}

	static final class DirectExecutor implements Executor {

		@Override
		public <I, O> O run(ActionSite<I, O> site, Object[] args, I value) {
			try {
				return site.output.rawType.cast(
						Utils.produce(site.action, site.owner, args,
								e -> SupplyFailed.valueOf(e, site.action)));
			} catch (SupplyFailed e) {
				Exception ex = e;
				if (e.getCause() instanceof Exception) {
					ex = (Exception) e.getCause();
				}
				throw new ActionExecutionFailed(
						"Exception on invocation of the action", ex);
			}
		}
	}

	static final class ActionSupplier implements Supplier<Action<?, ?>> {

		/**
		 * A list of discovered methods for each implementation class.
		 */
		private final Map<Class<?>, Method[]> cachedMethods = new ConcurrentHashMap<>();
		/**
		 * All already created {@link Action}s identified by a unique function
		 * signature.
		 */
		private final Map<String, Action<?, ?>> cachedActions = new ConcurrentHashMap<>();

		private final Injector injector;
		private final ProducesBy actionMirror;
		private final Executor executor;
		private final Class<?>[] implementationClasses;

		public ActionSupplier(Injector injector) {
			this.injector = injector;
			this.executor = injector.resolve(Executor.class);
			this.implementationClasses = injector.resolve(
					Plugins.class).forPoint(Action.class);
			this.actionMirror = injector.resolve(
					dependency(ACTION_MIRROR).injectingInto(
							ActionSupplier.class));
		}

		@Override
		public Action<?, ?> supply(Dependency<? super Action<?, ?>> dep,
				Injector context) {
			Type<? super Action<?, ?>> type = dep.type();
			return provide(type.parameter(0), type.parameter(1));
		}

		@SuppressWarnings("unchecked")
		private <I, O> Action<I, O> provide(Type<I> input, Type<O> output) {
			final String key = input + "->" + output; // haskell like function signature
			return (Action<I, O>) cachedActions.computeIfAbsent(key,
					k -> newAction(input, output));
		}

		private <I, O> Action<?, ?> newAction(Type<I> input, Type<O> output) {
			Method method = resolveAction(input, output);
			Object impl = injector.resolve(method.getDeclaringClass());
			return new ExecutorRunAction<>(injector, executor,
					new ActionSite<>(impl, method, input, output));
		}

		private <I, O> Method resolveAction(Type<I> input, Type<O> output) {
			for (Class<?> impl : implementationClasses) {
				Method action = arrayFindFirst(actionsIn(impl),
						a -> isActionForTypes(a, input, output));
				if (action != null)
					return action;
			}
			throw new UnresolvableDependency.NoMethodForDependency(output,
					input);
		}

		private static <I, O> boolean isActionForTypes(Method candidate,
				Type<I> input, Type<O> output) {
			return returnType(candidate).equalTo(output)
				&& (input.equalTo(Type.VOID)
					&& candidate.getParameterCount() == 0 // no input => no params
					|| arrayFindFirst(parameterTypes(candidate),
							t -> t.equalTo(input)) != null);
		}

		private Method[] actionsIn(Class<?> impl) {
			return cachedMethods.computeIfAbsent(impl,
					k -> actionMirror.reflect(impl));
		}
	}

	private static final class ExecutorRunAction<I, O> implements Action<I, O> {

		private final Injector injector;
		private final Executor executor;

		private final ActionSite<I, O> site;
		private final InjectionSite injection;
		private final int inputIndex;

		ExecutorRunAction(Injector injector, Executor executor,
				ActionSite<I, O> site) {
			this.injector = injector;
			this.executor = executor;
			this.site = site;
			Type<?>[] types = parameterTypes(site.action);
			this.injection = new InjectionSite(injector,
					dependency(site.output).injectingInto(
							site.action.getDeclaringClass()),
					Hint.match(types, Hint.constantNull(site.input)));
			this.inputIndex = asList(types).indexOf(site.input);
		}

		@Override
		public O run(I input) throws ActionExecutionFailed {
			Object[] args;
			try {
				args = injection.args(injector);
			} catch (UnresolvableDependency e) {
				throw new ActionExecutionFailed(
						"Failed to provide all implicit arguments", e);
			}
			if (inputIndex >= 0)
				args[inputIndex] = input;
			return executor.run(site, args, input);
		}
	}
}
