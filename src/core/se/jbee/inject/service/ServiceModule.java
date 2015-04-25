/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.service;

import static java.util.Arrays.asList;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Dependency.pluginsFor;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.parameterTypes;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Type.returnType;
import static se.jbee.inject.container.Scoped.APPLICATION;
import static se.jbee.inject.container.Scoped.DEPENDENCY_TYPE;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.bootstrap.BoundParameter;
import se.jbee.inject.bootstrap.InjectionSite;
import se.jbee.inject.bootstrap.Inspect;
import se.jbee.inject.bootstrap.Inspector;
import se.jbee.inject.bootstrap.Invoke;
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <I,O> Dependency<ServiceMethod<I,O>> serviceDependency(Type<I> paramType, Type<O> returnType) {
		Type type = raw(ServiceMethod.class).parametized(paramType, returnType);
		return dependency(type);
	}	
	
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
			asDefault().per( DEPENDENCY_TYPE ).starbind( ServiceMethod.class ).toSupplier( ServiceSupplier.class );
			asDefault().per( APPLICATION ).bind( SERVICE_INSPECTOR ).to( Inspect.all().methods() );
		}

	}

	static final class ServiceSupplier
			implements Supplier<ServiceMethod<?, ?>> {

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

		public ServiceSupplier( Injector injector ) {
			super();
			this.injector = injector;
			this.serviceClasses = injector.resolve( pluginsFor(ServiceMethod.class) );
			this.inspect = injector.resolve( dependency( SERVICE_INSPECTOR ).injectingInto(	ServiceSupplier.class ) );
		}
		
		@Override
		public ServiceMethod<?, ?> supply( Dependency<? super ServiceMethod<?, ?>> dependency, Injector injector ) {
			Type<? super ServiceMethod<?, ?>> type = dependency.type();
			return provide( type.parameter( 0 ), type.parameter( 1 ) );
		}

		@SuppressWarnings ( "unchecked" )
		private <I, O> ServiceMethod<I, O> provide( Type<I> parameterType, Type<O> returnType ) {
			String signatur = parameterType + "->" + returnType; // haskell like function signature
			ServiceMethod<?, ?> service = serviceCache.get( signatur );
			if ( service == null ) {
				synchronized ( serviceCache ) {
					service = serviceCache.get( signatur );
					if ( service == null ) {
						Method method = resolveServiceMethod( parameterType, returnType );
						Object implementor = injector.resolve( dependency( method.getDeclaringClass() ) );
						service = new InterceptableServiceMethod<I, O>( implementor, method, parameterType, returnType, injector );
						serviceCache.put( signatur, service );
					}
				}
			}
			return (ServiceMethod<I, O>) service;
		}

		private <I, O> Method resolveServiceMethod( Type<I> parameterType, Type<O> returnType ) {
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
			throw new UnresolvableDependency.NoMethodForDependency( returnType, parameterType );
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
	
	static int parameterIndex(Method method, Type<?> parameterType) {
		Type<?>[] types = parameterTypes(method);
		for (int i = 0; i < types.length; i++) {
			if (types[i].equalTo(parameterType))
				return i;
		}
		return -1;
	}

	private static final class InterceptableServiceMethod<I, O>
			implements ServiceMethod<I, O> {

		private final Object owner;
		private final Method method;

		private final Type<I> parameterType;
		private final Type<O> returnType;
		private final ServiceInvocation<?>[] invocations;
		
		private final Injector injector;
		private final InjectionSite injection;
		private final int parameterIndex;

		InterceptableServiceMethod( Object owner, Method method, Type<I> parameterType, Type<O> returnType, Injector injector ) {
			super();
			this.method = Metaclass.accessible(method);
			this.parameterType = parameterType;
			this.returnType = returnType;
			this.invocations = injector.resolve(dependency(ServiceInvocation[].class));
			this.injector = injector;
			Type<?>[] types = Type.parameterTypes(method);
			this.injection = new InjectionSite(dependency(returnType).injectingInto(method.getDeclaringClass()), injector, BoundParameter.bind(types, BoundParameter.constant(parameterType, null)));
			if ( !Modifier.isStatic( method.getModifiers() ) && owner == null ) {
				owner = injector.resolve( Dependency.dependency( method.getDeclaringClass() ) );
			}
			this.owner = owner;
			this.parameterIndex = asList(types).indexOf(parameterType);
		}
		
		@Override
		public O invoke( I params ) throws ServiceMalfunction {
			Object[] state = before( params );
			O res = null;
			try {
				Object[] args = injection.args(injector);
				if (parameterIndex >= 0) {
					args[parameterIndex] = params;
				}
				res = returnType.rawType.cast(Invoke.method(method, owner, args));
			} catch ( Exception e ) {
				afterException( params, e, state );
				throw new ServiceMalfunction(method.getDeclaringClass().getSimpleName()+"#"+method.getName()+" failed: "+e.getMessage(), e );
			}
			after( params, res, state );
			return res;
		}
		
		@SuppressWarnings ( "unchecked" )
		private <T> void afterException( I params, Exception e, Object[] states ) {
			if ( invocations.length == 0 ) {
				return;
			}
			for ( int i = 0; i < invocations.length; i++ ) {
				try {
					ServiceInvocation<T> inv = (ServiceInvocation<T>) invocations[i];
					inv.afterException( parameterType, params, returnType, e, (T) states[i] );
				} catch ( RuntimeException re ) {
					// warn that invocation before had thrown an exception
				}
			}
		}

		private Object[] before( I params ) {
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
		private <T> void after( I param, O res, Object[] states ) {
			if ( invocations.length == 0 ) {
				return;
			}
			for ( int i = 0; i < invocations.length; i++ ) {
				try {
					ServiceInvocation<T> inv = (ServiceInvocation<T>) invocations[i];
					inv.after( parameterType, param, returnType, res, (T)states[i] );
				} catch ( RuntimeException e ) {
					// warn that invocation before had thrown an exception
				}
			}
		}

		@Override
		public String toString() {
			return method.getDeclaringClass().getSimpleName() + ": " + returnType + " "
					+ method.getName() + "(" + parameterType + ")";
		}
	}
}
