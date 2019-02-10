/*
 *  Copyright (c) 2012-2017, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.action;

import static java.util.Arrays.asList;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Dependency.pluginsFor;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.parameterTypes;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Type.returnType;
import static se.jbee.inject.bootstrap.Metaclass.accessible;
import static se.jbee.inject.container.Scoped.APPLICATION;
import static se.jbee.inject.container.Scoped.DEPENDENCY_TYPE;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.UnresolvableDependency.SupplyFailed;
import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.bootstrap.BoundParameter;
import se.jbee.inject.bootstrap.InjectionSite;
import se.jbee.inject.bootstrap.Inspect;
import se.jbee.inject.bootstrap.Inspector;
import se.jbee.inject.bootstrap.Module;
import se.jbee.inject.bootstrap.Supply;
import se.jbee.inject.container.Scoped;
import se.jbee.inject.container.Supplier;

/**
 * When binding {@link Action}s this {@link Module} can be extended.
 *
 * It provides procedure-related bind methods.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class ActionModule extends BinderModule {

	/**
	 * The {@link Inspector} picks the {@link Method}s that are used to implement
	 * {@link Action}s. This abstraction allows to customise what methods are bound as
	 * {@link Action}s. The {@link Inspector#methodsIn(Class)} should return all methods in
	 * the given {@link Class} that should be used to implement a {@link Action}.
	 */
	static final Instance<Inspector> ACTION_INSPECTOR = instance(named(Action.class), raw(Inspector.class));

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <I,O> Dependency<Action<I,O>> actionDependency(Type<I> input, Type<O> output) {
		Type type = raw(Action.class).parametized(input, output);
		return dependency(type);
	}

	protected final void bindActionsIn( Class<?> impl ) {
		plug(impl).into(Action.class);
	}

	protected final void discoverActionsBy(Inspector inspector) {
		bind(ACTION_INSPECTOR).to(inspector);
	}

	protected ActionModule() {
		super(Scoped.APPLICATION, ActionBaseModule.class);
	}

	private static final class ActionBaseModule extends BinderModule {

		@Override
		public void declare() {
			asDefault().per(DEPENDENCY_TYPE).starbind(Action.class).toSupplier(ActionSupplier.class);
			asDefault().per(APPLICATION).bind(ACTION_INSPECTOR).to(Inspect.all().methods());
			asDefault().per(APPLICATION).bind(Executor.class).to(DirectExecutor.class);
		}

	}

	static final class DirectExecutor implements Executor {

		@Override
		public <I, O> O exec(Object impl, Method action, Object[] args,	Type<O> output, Type<I> input, I value) {
			try {
				return output.rawType.cast(Supply.method(action, impl, args));
			} catch (SupplyFailed e) {
				Exception ex = e;
				if ( e.getCause() instanceof Exception ) {
					ex = (Exception) e.getCause();
				}
				throw new ActionMalfunction("Exception on invocation of the action", ex);
			}
		}
	}

	static final class ActionSupplier implements Supplier<Action<?, ?>> {

		/**
		 * A list of discovered methods for each implementation class.
		 */
		private final Map<Class<?>, Method[]> cachedMethods = new ConcurrentHashMap<>();
		/**
		 * All already created {@link Action}s identified by a unique function signature.
		 */
		private final Map<String, Action<?, ?>> cachedActions = new ConcurrentHashMap<>();

		private final Injector injector;
		private final Inspector inspect;
		private final Executor executor;
		private final Class<?>[] implementationClasses;

		public ActionSupplier(Injector injector) {
			this.injector = injector;
			this.executor = injector.resolve(Executor.class);
			this.implementationClasses = injector.resolve(pluginsFor(Action.class));
			this.inspect = injector.resolve( 
					dependency(ACTION_INSPECTOR).injectingInto(ActionSupplier.class));
		}

		@Override
		public Action<?, ?> supply(Dependency<? super Action<?, ?>> dep, Injector injector) {
			Type<? super Action<?, ?>> type = dep.type();
			return provide(type.parameter(0), type.parameter(1));
		}

		@SuppressWarnings ( "unchecked" )
		private <I, O> Action<I, O> provide( Type<I> input, Type<O> output ) {
			final String key = input + "->" + output; // haskell like function signature
			return (Action<I, O>) cachedActions.computeIfAbsent(key, k -> newAction(input, output));
		}

		private <I, O> Action<?, ?> newAction(Type<I> input, Type<O> output) {
			Action<?, ?> action;
			Method method = resolveAction( input, output );
			Object impl = injector.resolve( method.getDeclaringClass() );
			action = new ExecutorRunAction<>(impl, method, input, output, executor, injector);
			return action;
		}

		private <I, O> Method resolveAction( Type<I> input, Type<O> output ) {
			for ( Class<?> impl : implementationClasses ) {
				for ( Method action : actionsIn( impl ) ) {
					Type<?> rt = returnType( action );
					if ( rt.equalTo( output ) ) {
						if ( input.equalTo( Type.VOID ) ) {
							if ( action.getParameterTypes().length == 0 ) {
								return action;
							}
						} else {
							for ( Type<?> pt : parameterTypes( action ) ) {
								if ( pt.equalTo( input ) ) {
									return action;
								}
							}
						}
					}
				}
			}
			throw new UnresolvableDependency.NoMethodForDependency( output, input );
		}

		private Method[] actionsIn( Class<?> impl ) {
			return cachedMethods.computeIfAbsent(impl, k -> inspect.methodsIn(impl));
		}
	}

	private static final class ExecutorRunAction<I,O> implements Action<I, O> {

		private final Object impl;
		private final Method action;
		private final Type<I> input;
		private final Type<O> output;

		private final Executor executor;
		private final Injector injector;

		private final InjectionSite injection;
		private final int inputIndex;

		ExecutorRunAction(Object impl, Method action, Type<I> input, Type<O> output, Executor executor, Injector injector) {
			this.impl = impl;
			this.action = accessible(action);
			this.input = input;
			this.output = output;
			this.executor = executor;
			this.injector = injector;
			Type<?>[] types = parameterTypes(action);
			this.injection = new InjectionSite(
					dependency(output).injectingInto(action.getDeclaringClass()), 
					injector, 
					BoundParameter.bind(types, BoundParameter.constant(input, null)));
			this.inputIndex = asList(types).indexOf(input);
		}

		@Override
		public O exec(I input) throws ActionMalfunction {
			Object[] args = null;
			try {
				args = injection.args(injector);
			} catch (UnresolvableDependency e) {
				throw new ActionMalfunction("Failed to provide all implicit arguments", e);
			}
			if (inputIndex >= 0) {
				args[inputIndex] = input;
			}
			return executor.exec(impl, action, args, output, this.input, input);
		}
	}
}