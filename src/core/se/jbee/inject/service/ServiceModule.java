/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.service;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Dependency.pluginsFor;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.parameterTypes;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Type.returnType;
import static se.jbee.inject.container.Scoped.APPLICATION;
import static se.jbee.inject.container.Scoped.DEPENDENCY_TYPE;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import se.jbee.inject.DIRuntimeException;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Injectron;
import se.jbee.inject.Instance;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.bootstrap.Inspect;
import se.jbee.inject.bootstrap.Inspector;
import se.jbee.inject.bootstrap.Metaclass;
import se.jbee.inject.bootstrap.Module;
import se.jbee.inject.container.Scoped;

/**
 * When binding {@link ServiceMethod}s this {@link Module} can be extended.
 * 
 * It provides service-related build methods.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class ServiceModule
		extends BinderModule {

	/**
	 * The {@link Inspector} picks the {@link Method}s that are used to implement
	 * {@link ServiceMethod}s. This abstraction allows to customize what methods are bound as
	 * {@link ServiceMethod}s. The {@link Inspector#methodsIn(Class)} should return all methods in
	 * the given {@link Class} that should be used to implement a {@link ServiceMethod}.
	 */
	public static final Instance<Inspector> SERVICE_INSPECTOR = instance( named( ServiceMethod.class), raw( Inspector.class ) );

	protected final void bindServiceMethodsIn( Class<?> service ) {
		plug(service).into(ServiceMethod.class);
	}
	
	protected final void bindInvocationHandler( Class<? extends ServiceInvocation<?>> invocationHandler ) {
		plug(invocationHandler).into(ServiceInvocation.class);
	}

	protected final void bindServiceInspectorTo( Inspector inspector ) {
		bind( SERVICE_INSPECTOR ).to( inspector );
	}

	protected ServiceModule() {
		super(Scoped.APPLICATION, ServiceMethodModule.class);
	}

	private static final class ServiceMethodModule
			extends BinderModule {

		@Override
		public void declare() {
			per( APPLICATION ).bind( ServiceProvider.class ).toSupplier( ServiceProviderSupplier.class );
			per( DEPENDENCY_TYPE ).starbind( ServiceMethod.class ).toSupplier( ServiceSupplier.class );
			asDefault().per( APPLICATION ).bind( SERVICE_INSPECTOR ).to( Inspect.all().methods() );
		}

	}

	private static final class ServiceProviderSupplier
			implements Supplier<ServiceProvider> {

		@Override
		public ServiceProvider supply( Dependency<? super ServiceProvider> dependency, Injector injector ) {
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
		 * All already created {@link ServiceMethod}s identified by a unique function signature.
		 */
		private final Map<String, ServiceMethod<?, ?>> serviceCache = new HashMap<String, ServiceMethod<?, ?>>();

		private final Injector injector;
		private final Inspector inspect;
		private final Class<?>[] serviceClasses;

		ServiceMethodProvider( Injector injector ) {
			super();
			this.injector = injector;
			this.serviceClasses = injector.resolve( pluginsFor(ServiceMethod.class) );
			this.inspect = injector.resolve( dependency( SERVICE_INSPECTOR ).injectingInto(	ServiceProvider.class ) );
		}

		@SuppressWarnings ( "unchecked" )
		@Override
		public <P, R> ServiceMethod<P, R> provide( Type<P> parameterType, Type<R> returnType ) {
			String signatur = parameterType + "->" + returnType; // haskell like function signature
			ServiceMethod<?, ?> service = serviceCache.get( signatur );
			if ( service == null ) {
				synchronized ( serviceCache ) {
					service = serviceCache.get( signatur );
					if ( service == null ) {
						service = create( resolveServiceMethod( parameterType, returnType ),
								parameterType, returnType, injector );
						serviceCache.put( signatur, service );
					}
				}
			}
			return (ServiceMethod<P, R>) service;
		}

		private static <P, T> ServiceMethod<P, T> create( Method service, Type<P> parameterType,
				Type<T> returnType, Injector injector ) {
			Object implementor = injector.resolve( dependency( service.getDeclaringClass() ) );
			return new PreresolvingServiceMethod<P, T>( implementor, service, parameterType,
					returnType, injector );
		}

		private <P, T> Method resolveServiceMethod( Type<P> parameterType, Type<T> returnType ) {
			for ( Class<?> service : serviceClasses ) {
				for ( Method sm : serviceClassMethods( service ) ) {
					Type<?> rt = returnType( sm );
					if ( rt.equalTo( returnType ) ) {
						if ( parameterType.equalTo( Type.VOID ) ) {
							if ( sm.getParameterTypes().length == 0 ) {
								return sm;
							}
						} else {
							for ( Type<?> pt : parameterTypes( sm ) ) {
								if ( pt.equalTo( parameterType ) ) {
									return sm;
								}
							}
						}
					}
				}
			}
			throw new DIRuntimeException.NoSuchFunctionException( returnType, parameterType );
		}

		private Method[] serviceClassMethods( Class<?> service ) {
			Method[] methods = methodsCache.get( service );
			if ( methods != null ) {
				return methods;
			}
			synchronized ( methodsCache ) {
				methods = methodsCache.get( service );
				if ( methods == null ) {
					methods = inspect.methodsIn( service );
					methodsCache.put( service, methods );
				}
			}
			return methods;
		}

	}

	private static final class ServiceSupplier
			implements Supplier<ServiceMethod<?, ?>> {

		@Override
		public ServiceMethod<?, ?> supply( Dependency<? super ServiceMethod<?, ?>> dependency,
				Injector injector ) {
			ServiceProvider serviceProvider = injector.resolve( dependency.anyTyped( ServiceProvider.class ) );
			Type<? super ServiceMethod<?, ?>> type = dependency.getType();
			return serviceProvider.provide( type.parameter( 0 ), type.parameter( 1 ) );
		}
	}

	private static final class PreresolvingServiceMethod<P, T>
			implements ServiceMethod<P, T> {

		private final Object implementor;
		private final Method method;
		private final Type<P> parameterType;
		private final Type<T> returnType;
		private final Injector injector;
		private final Injectron<?>[] argumentInjectrons;
		private final Type<?>[] parameterTypes;
		private final Object[] argumentTemplate;
		private final ServiceInvocation<?>[] invocations;

		PreresolvingServiceMethod( Object implementor, Method service, Type<P> parameterType,
				Type<T> returnType, Injector injector ) {
			super();
			this.implementor = implementor;
			this.method = Metaclass.accessible( service );
			this.parameterType = parameterType;
			this.returnType = returnType;
			this.injector = injector;
			this.parameterTypes = parameterTypes( method );
			this.argumentInjectrons = argumentInjectrons();
			this.argumentTemplate = argumentTemplate();
			this.invocations = injector.resolve(dependency(ServiceInvocation[].class));
		}

		private Object[] argumentTemplate() {
			Object[] template = new Object[parameterTypes.length];
			for ( int i = 0; i < template.length; i++ ) {
				Injectron<?> injectron = argumentInjectrons[i];
				if ( injectron != null && injectron.getInfo().expiry.isNever() ) {
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
				res = returnType.getRawType().cast( method.invoke( implementor, args ) );
			} catch ( Exception e ) {
				if ( e instanceof InvocationTargetException ) {
					Throwable t = ( (InvocationTargetException) e ).getTargetException();
					if ( t instanceof Exception ) {
						e = (Exception) t;
					}
				}
				afterException( params, e, state );
				throw new RuntimeException( "Failed to invoke service: " + this + " \n"	+ e.getMessage(), e );
			}
			after( params, res, state );
			return res;
		}
		
		@SuppressWarnings ( "unchecked" )
		private <I> void afterException( P params, Exception e, Object[] states ) {
			if ( invocations.length == 0 ) {
				return;
			}
			for ( int i = 0; i < invocations.length; i++ ) {
				try {
					ServiceInvocation<I> inv = (ServiceInvocation<I>) invocations[i];
					inv.afterException( parameterType, params, returnType, e, (I) states[i] );
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
			for ( int i = 0; i < invocations.length; i++ ) {
				try {
					invokeState[i] = invocations[i].before( parameterType, params, returnType );
				} catch ( RuntimeException e ) {
					// warn that invocation before had thrown an exception
				}
			}
			return invokeState;
		}

		@SuppressWarnings ( "unchecked" )
		private <I> void after( P param, T res, Object[] states ) {
			if ( invocations.length == 0 ) {
				return;
			}
			for ( int i = 0; i < invocations.length; i++ ) {
				try {
					ServiceInvocation<I> inv = (ServiceInvocation<I>) invocations[i];
					inv.after( parameterType, param, returnType, res, (I)states[i] );
				} catch ( RuntimeException e ) {
					// warn that invocation before had thrown an exception
				}
			}
		}

		private Injectron<?>[] argumentInjectrons() {
			Injectron<?>[] res = new Injectron<?>[parameterTypes.length];
			for ( int i = 0; i < res.length; i++ ) {
				Type<?> paramType = parameterTypes[i];
				res[i] = paramType.equalTo( parameterType )
					? null
					: injector.resolve( dependency( raw( Injectron.class ).parametized( paramType ) ) );
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
