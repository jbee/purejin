package de.jbee.inject.service;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Name.named;
import static de.jbee.inject.Scoped.APPLICATION;
import static de.jbee.inject.Scoped.DEPENDENCY_TYPE;
import static de.jbee.inject.Source.source;
import static de.jbee.inject.Type.parameterTypes;
import static de.jbee.inject.Type.raw;
import static de.jbee.inject.Type.returnType;
import static de.jbee.inject.bind.Bootstrap.nonnullThrowsReentranceException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import de.jbee.inject.Dependency;
import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Name;
import de.jbee.inject.Supplier;
import de.jbee.inject.Type;
import de.jbee.inject.TypeReflector;
import de.jbee.inject.bind.Binder;
import de.jbee.inject.bind.BinderModule;
import de.jbee.inject.bind.Bindings;
import de.jbee.inject.bind.Bootstrapper;
import de.jbee.inject.bind.Bundle;
import de.jbee.inject.bind.Module;
import de.jbee.inject.bind.Binder.RootBinder;
import de.jbee.inject.bind.Binder.TypedBinder;

/**
 * When binding {@link ServiceMethod}s this {@link Module} can be extended.
 * 
 * It provides service-related build methods.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public abstract class ServiceModule
		implements Module, Bundle {

	private static final String SERVICE_NAME_PREFIX = "Service-";

	protected final void bindServiceMethodsIn( Class<?> service ) {
		String name = SERVICE_NAME_PREFIX + service.getCanonicalName();
		binder.multibind( named( name ), Class.class ).to( service );
	}

	protected final <T> TypedBinder<T> superbind( Class<T> service ) {
		return binder.per( DEPENDENCY_TYPE ).starbind( service );
	}

	private RootBinder binder;

	@Override
	public final void bootstrap( Bootstrapper bootstrap ) {
		bootstrap.install( ServiceMethodModule.class );
		bootstrap.install( this );
	}

	@Override
	public final void declare( Bindings bindings ) {
		nonnullThrowsReentranceException( binder );
		binder = Binder.create( bindings, source( getClass() ) );
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
				DependencyResolver context ) {
			return new ServiceMethodProvider( context );
		}

	}

	private static final class ServiceMethodProvider
			implements ServiceProvider {

		private final Map<Class<?>, Method[]> methodsCache = new IdentityHashMap<Class<?>, Method[]>();
		private final Map<String, Method> methodCache = new HashMap<String, Method>();

		private final DependencyResolver context;
		private final Class<?>[] serviceClasses;
		private final ServiceStrategy strategy;

		ServiceMethodProvider( DependencyResolver context ) {
			super();
			this.context = context;
			this.serviceClasses = resolveServiceClasses( context );
			this.strategy = context.resolve( dependency( ServiceStrategy.class ).injectingInto(
					ServiceMethodProvider.class ) );
		}

		private static Class<?>[] resolveServiceClasses( DependencyResolver context ) {
			Dependency<Class[]> serviceClassesDependency = dependency(
					raw( Class[].class ).parametizedAsLowerBounds() ).named(
					Name.prefixed( SERVICE_NAME_PREFIX ) ).injectingInto(
					ServiceMethodProvider.class );
			return context.resolve( serviceClassesDependency );
		}

		@Override
		public <P, R> ServiceMethod<P, R> provide( Type<P> parameterType, Type<R> returnType ) {
			Method service = resolveServiceMethod( parameterType, returnType );
			return create( service, parameterType.getRawType(), returnType.getRawType(), context );
		}

		private <P, T> ServiceMethod<P, T> create( Method service, Class<P> parameterType,
				Class<T> returnType, DependencyResolver context ) {
			return new LazyServiceMethod<P, T>(
					TypeReflector.newInstance( service.getDeclaringClass() ), service,
					parameterType, returnType, context );
		}

		private <P, T> Method serviceMethod( Type<P> parameterType, Type<T> returnType ) {
			String signatur = parameterType + "-" + returnType;
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
			//FIXME primitives types aren't covered...but ... they can be put as parameter for Type 
			return null;
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

	//OPEN move this to test-code since it is more of an example using the SM directly as the service interface
	private static class ServiceSupplier
			implements Supplier<ServiceMethod<?, ?>> {

		@Override
		public ServiceMethod<?, ?> supply( Dependency<? super ServiceMethod<?, ?>> dependency,
				DependencyResolver context ) {
			ServiceProvider serviceResolver = context.resolve( dependency.anyTyped( ServiceProvider.class ) );
			Type<?>[] parameters = dependency.getType().getParameters();
			return serviceResolver.provide( parameters[0], parameters[1] );
		}
	}

	static class LazyServiceMethod<P, T>
			implements ServiceMethod<P, T> {

		private final Object object;
		private final Method method;
		private final Class<P> parameterType;
		private final Class<T> returnType;
		private final DependencyResolver context;
		private final Type<?>[] parameterTypes;

		LazyServiceMethod( Object object, Method service, Class<P> parameterType,
				Class<T> returnType, DependencyResolver context ) {
			super();
			this.object = object;
			this.method = service;
			this.parameterType = parameterType;
			this.returnType = returnType;
			this.context = context;
			this.parameterTypes = parameterTypes( service );
		}

		@Override
		public T invoke( P params ) {
			Object[] args = actualArguments( params );
			try {
				method.setAccessible( true ); //TODO just do it once
				return returnType.cast( method.invoke( object, args ) );
			} catch ( Exception e ) {
				throw new RuntimeException( "Failed to invoke service:\n" + e.getMessage(), e );
			}
		}

		private Object[] actualArguments( P params ) {
			Object[] args = new Object[parameterTypes.length];
			for ( int i = 0; i < args.length; i++ ) {
				Type<?> paramType = parameterTypes[i];
				if ( paramType.getRawType() == parameterType ) {
					args[i] = params;
				} else {
					// TODO add information from method (like annotations ?)
					args[i] = context.resolve( dependency( paramType ) );
				}
			}
			return args;
		}

	}
}
