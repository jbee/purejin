/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject.service;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Source.source;
import static de.jbee.inject.Type.parameterTypes;
import static de.jbee.inject.Type.raw;
import static de.jbee.inject.Type.returnType;
import static de.jbee.inject.util.Scoped.APPLICATION;
import static de.jbee.inject.util.Scoped.DEPENDENCY_TYPE;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import de.jbee.inject.DIRuntimeException;
import de.jbee.inject.Dependency;
import de.jbee.inject.Injector;
import de.jbee.inject.Injectron;
import de.jbee.inject.Supplier;
import de.jbee.inject.Type;
import de.jbee.inject.bind.Binder;
import de.jbee.inject.bind.BinderModule;
import de.jbee.inject.bind.Bindings;
import de.jbee.inject.bind.Bootstrapper;
import de.jbee.inject.bind.BootstrappingModule;
import de.jbee.inject.bind.Bundle;
import de.jbee.inject.bind.ConstructionStrategy;
import de.jbee.inject.bind.Extend;
import de.jbee.inject.bind.Module;
import de.jbee.inject.bind.Binder.RootBinder;
import de.jbee.inject.bind.Binder.TypedBinder;
import de.jbee.inject.service.ServiceInvocation.ServiceInvocationExtension;
import de.jbee.inject.service.ServiceMethod.ServiceClassExtension;
import de.jbee.inject.util.Scoped;
import de.jbee.inject.util.TypeReflector;
import de.jbee.inject.util.Value;

/**
 * When binding {@link ServiceMethod}s this {@link Module} can be extended.
 * 
 * It provides service-related build methods.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public abstract class ServiceModule
		implements Module, Bundle {

	protected final void bindServiceMethodsIn( Class<?> service ) {
		binder.extend( ServiceClassExtension.class, service );
	}

	protected final void extend( ServiceInvocationExtension type,
			Class<? extends ServiceInvocation<?>> invocation ) {
		//TODO we need a special binder for this
		binder.extend( type, invocation );
	}

	protected final <T> TypedBinder<T> starbind( Class<T> service ) {
		return binder.per( DEPENDENCY_TYPE ).starbind( service );
	}

	private RootBinder binder;

	@Override
	public final void bootstrap( Bootstrapper bootstrap ) {
		bootstrap.install( ServiceMethodModule.class );
		bootstrap.install( this );
	}

	@Override
	public void declare( Bindings bindings, ConstructionStrategy strategy ) {
		BootstrappingModule.nonnullThrowsReentranceException( binder );
		binder = Binder.create( bindings, strategy, source( getClass() ), Scoped.APPLICATION );
		declare();
	}

	protected abstract void declare();

	static class ServiceMethodModule
			extends BinderModule {

		static final ServiceStrategy DEFAULT_SERVICE_STRATEGY = new BuildinServiceStrategy();

		@Override
		public void declare() {
			per( APPLICATION ).bind( ServiceProvider.class ).toSupplier(
					ServiceProviderSupplier.class );
			per( DEPENDENCY_TYPE ).starbind( ServiceMethod.class ).toSupplier(
					ServiceSupplier.class );
			asDefault().per( APPLICATION ).bind( ServiceStrategy.class ).to(
					DEFAULT_SERVICE_STRATEGY );
		}

	}

	private static final class BuildinServiceStrategy
			implements ServiceStrategy {

		BuildinServiceStrategy() {
			// make visible
		}

		@Override
		public Method[] serviceMethodsIn( Class<?> serviceClass ) {
			return serviceClass.getDeclaredMethods();
		}

	}

	private static final class ServiceProviderSupplier
			implements Supplier<ServiceProvider> {

		@Override
		public ServiceProvider supply( Dependency<? super ServiceProvider> dependency,
				Injector injector ) {
			return new ServiceMethodProvider( injector );
		}

	}

	private static final class ServiceMethodProvider
			implements ServiceProvider {

		/**
		 * A list of service methods for each service class.
		 */
		private final Map<Class<?>, Method[]> methodsCache = new IdentityHashMap<Class<?>, Method[]>();
		/**
		 * All already created service methods identified by a unique function signature.
		 * 
		 * OPEN why not put the {@link ServiceMethod} in cache ?
		 */
		private final Map<String, Method> methodCache = new HashMap<String, Method>();

		private final Injector context;
		private final Class<?>[] serviceClasses;
		private final ServiceStrategy strategy;

		ServiceMethodProvider( Injector context ) {
			super();
			this.context = context;
			this.serviceClasses = context.resolve( Extend.dependency( ServiceClassExtension.class ) );
			this.strategy = context.resolve( dependency( ServiceStrategy.class ).injectingInto(
					ServiceMethodProvider.class ) );
		}

		@Override
		public <P, R> ServiceMethod<P, R> provide( Type<P> parameterType, Type<R> returnType ) {
			Method service = serviceMethod( parameterType, returnType );
			return create( service, parameterType, returnType, context );
		}

		private <P, T> ServiceMethod<P, T> create( Method service, Type<P> parameterType,
				Type<T> returnType, Injector context ) {
			return new PreresolvingServiceMethod<P, T>(
					TypeReflector.newInstance( service.getDeclaringClass() ), service,
					parameterType, returnType, context );
		}

		private <P, T> Method serviceMethod( Type<P> parameterType, Type<T> returnType ) {
			String signatur = parameterType + "->" + returnType; // haskell like function signature
			Method method = methodCache.get( signatur );
			if ( method != null ) {
				return method;
			}
			synchronized ( methodCache ) {
				method = methodCache.get( signatur );
				if ( method == null ) {
					method = resolveServiceMethod( parameterType, returnType );
					methodCache.put( signatur, method );
				}
			}
			return method;
		}

		private <P, T> Method resolveServiceMethod( Type<P> parameterType, Type<T> returnType ) {
			for ( Class<?> service : serviceClasses ) {
				for ( Method sm : serviceClassMethods( service ) ) {
					Type<?> rt = returnType( sm );
					if ( rt.equalTo( returnType ) ) {
						for ( Type<?> pt : parameterTypes( sm ) ) {
							if ( pt.equalTo( parameterType ) ) {
								return sm;
							}
						}
					}
				}
			}
			throw new DIRuntimeException.NoSuchMethodException( returnType, parameterType );
		}

		private Method[] serviceClassMethods( Class<?> service ) {
			Method[] methods = methodsCache.get( service );
			if ( methods != null ) {
				return methods;
			}
			synchronized ( methodsCache ) {
				methods = methodsCache.get( service );
				if ( methods == null ) {
					methods = strategy.serviceMethodsIn( service );
					methodsCache.put( service, methods );
				}
			}
			return methods;
		}

	}

	private static class ServiceSupplier
			implements Supplier<ServiceMethod<?, ?>> {

		@Override
		public ServiceMethod<?, ?> supply( Dependency<? super ServiceMethod<?, ?>> dependency,
				Injector injector ) {
			ServiceProvider serviceProvider = injector.resolve( dependency.anyTyped( ServiceProvider.class ) );
			Type<?>[] parameters = dependency.getType().getParameters();
			return serviceProvider.provide( parameters[0], parameters[1] );
		}
	}

	private static class PreresolvingServiceMethod<P, T>
			implements ServiceMethod<P, T> {

		private final Object object;
		private final Method method;
		private final Type<P> parameterType;
		private final Type<T> returnType;
		private final Injector context;
		private final Type<?>[] parameterTypes;
		private final Injectron<?>[] argumentInjectrons;
		private final Object[] argumentTemplate;
		private final ServiceInvocation<?>[] invocations;

		PreresolvingServiceMethod( Object object, Method service, Type<P> parameterType,
				Type<T> returnType, Injector context ) {
			super();
			this.object = object;
			this.method = service;
			TypeReflector.makeAccessible( method );
			this.parameterType = parameterType;
			this.returnType = returnType;
			this.context = context;
			this.parameterTypes = parameterTypes( service );
			this.argumentInjectrons = argumentInjectrons();
			this.argumentTemplate = argumentTemplate();
			this.invocations = resolveInvocations( context );
		}

		private ServiceInvocation<?>[] resolveInvocations( Injector context ) {
			@SuppressWarnings ( "unchecked" )
			Class<? extends ServiceInvocation<?>>[] classes = context.resolve( Extend.dependency( ServiceInvocationExtension.class ) );
			ServiceInvocation<?>[] res = new ServiceInvocation<?>[classes.length];
			for ( int i = 0; i < res.length; i++ ) {
				res[i] = context.resolve( Dependency.dependency( classes[i] ) );
			}
			return res;
		}

		private Object[] argumentTemplate() {
			Object[] template = new Object[parameterTypes.length];
			for ( int i = 0; i < template.length; i++ ) {
				Injectron<?> injectron = argumentInjectrons[i];
				if ( injectron != null && injectron.getExpiry().isNever() ) {
					template[i] = instance( injectron, dependency( parameterTypes[i] ) );
				}
			}
			return template;
		}

		@Override
		public T invoke( P params ) {
			Object[] args = actualArguments( params );
			Object[] state = before( params );
			T res = null;
			try {
				res = returnType.getRawType().cast( method.invoke( object, args ) );
			} catch ( Exception e ) {
				if ( e instanceof InvocationTargetException ) {
					Throwable t = ( (InvocationTargetException) e ).getTargetException();
					if ( t instanceof Exception ) {
						e = (Exception) t;
					}
				}
				afterException( params, e, state );
				throw new RuntimeException( "Failed to invoke service: " + this + " \n"
						+ e.getMessage(), e );
			}
			after( params, res, state );
			return res;
		}

		private void afterException( P params, Exception e, Object[] states ) {
			if ( invocations.length == 0 ) {
				return;
			}
			final Value<P> paramValue = Value.value( parameterType, params );
			for ( int i = 0; i < invocations.length; i++ ) {
				try {
					afterException( invocations[i], states[i], paramValue, returnType, e );
				} catch ( RuntimeException re ) {
					// warn that invocation before had thrown an exception
				}
			}
		}

		private Object[] before( P params ) {
			if ( invocations.length == 0 ) {
				return null;
			}
			Object[] invokeState = new Object[invocations.length];
			Value<P> v = Value.value( parameterType, params );
			for ( int i = 0; i < invocations.length; i++ ) {
				try {
					invokeState[i] = invocations[i].before( v, returnType );
				} catch ( RuntimeException e ) {
					// warn that invocation before had thrown an exception
				}
			}
			return invokeState;
		}

		private void after( P param, T res, Object[] states ) {
			if ( invocations.length == 0 ) {
				return;
			}
			final Value<P> paramValue = Value.value( parameterType, param );
			final Value<T> resValue = Value.value( returnType, res );
			for ( int i = 0; i < invocations.length; i++ ) {
				try {
					after( invocations[i], states[i], paramValue, resValue );
				} catch ( RuntimeException e ) {
					// warn that invocation before had thrown an exception
				}
			}
		}

		@SuppressWarnings ( "unchecked" )
		private static <I, P, T> void after( ServiceInvocation<I> inv, Object state,
				Value<P> param, Value<T> result ) {
			inv.after( param, result, (I) state );
		}

		@SuppressWarnings ( "unchecked" )
		private static <I, P, T> void afterException( ServiceInvocation<I> inv, Object state,
				Value<P> param, Type<T> result, Exception e ) {
			inv.afterException( param, result, e, (I) state );
		}

		private Injectron<?>[] argumentInjectrons() {
			Injectron<?>[] res = new Injectron<?>[parameterTypes.length];
			for ( int i = 0; i < res.length; i++ ) {
				Type<?> paramType = parameterTypes[i];
				res[i] = paramType.equalTo( parameterType )
					? null
					: context.resolve( dependency( raw( Injectron.class ).parametized( paramType ) ) );
			}
			return res;
		}

		private Object[] actualArguments( P params ) {
			Object[] args = argumentTemplate.clone();
			for ( int i = 0; i < args.length; i++ ) {
				Type<?> paramType = parameterTypes[i];
				if ( paramType.equalTo( parameterType ) ) {
					args[i] = params;
				} else if ( args[i] == null ) {
					args[i] = instance( argumentInjectrons[i], dependency( paramType ) );
				}
			}
			return args;
		}

		@SuppressWarnings ( "unchecked" )
		private static <I> I instance( Injectron<I> injectron, Dependency<?> dependency ) {
			return injectron.instanceFor( (Dependency<? super I>) dependency );
		}

		@Override
		public String toString() {
			return method.getDeclaringClass().getSimpleName() + ": " + returnType + " "
					+ method.getName() + "(" + parameterType + ")";
		}
	}
}
