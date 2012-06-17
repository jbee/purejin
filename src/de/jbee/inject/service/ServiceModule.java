package de.jbee.inject.service;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Name.named;
import static de.jbee.inject.Scoped.APPLICATION;
import static de.jbee.inject.Source.source;
import static de.jbee.inject.Type.parameterTypes;
import static de.jbee.inject.Type.raw;
import static de.jbee.inject.Type.returnType;

import java.lang.reflect.Method;

import de.jbee.inject.Dependency;
import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Name;
import de.jbee.inject.Supplier;
import de.jbee.inject.Type;
import de.jbee.inject.TypeReflector;
import de.jbee.inject.bind.Binder;
import de.jbee.inject.bind.Bindings;
import de.jbee.inject.bind.Bootstrapper;
import de.jbee.inject.bind.Bundle;
import de.jbee.inject.bind.Module;
import de.jbee.inject.bind.PackageModule;
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

	protected final void bindServiceMethods( Class<?> service ) {
		String name = SERVICE_NAME_PREFIX + service.getCanonicalName();
		binder.multibind( named( name ), Class.class ).to( service );
	}

	protected final <T> TypedBinder<T> superbind( Class<T> service ) {
		return binder.superbind( service );
	}

	private RootBinder binder;

	@Override
	public final void bootstrap( Bootstrapper bootstrap ) {
		bootstrap.install( ServiceMethodModule.class );
		bootstrap.install( this );
	}

	@Override
	public final void configure( Bindings bindings ) {
		if ( binder != null ) {
			throw new IllegalStateException( "Reentrance not allowed!" );
		}
		binder = Binder.create( bindings, source( getClass() ) );
		configure();
	}

	protected abstract void configure();

	static class ServiceMethodModule
			extends PackageModule {

		@Override
		public void configure() {
			in( APPLICATION ).bind( ServiceProvider.class ).to( ServiceMethodProvider.class );
			// TODO use scope that leads to one instance per exact type (including generics)
			superbind( ServiceMethod.class ).toSupplier( ServiceSupplier.class );
		}

	}

	private static final class ServiceMethodProvider
			implements ServiceProvider {

		private Class<?>[] serviceTypes;

		@Override
		public <P, R> ServiceMethod<P, R> provide( Type<P> parameterType, Type<R> returnType,
				DependencyResolver context ) {
			if ( serviceTypes == null ) {
				resolveServiceTypes( context );
			}
			Method service = resolveServiceMethod( parameterType, returnType );
			return newServiceMethod( service, parameterType.getRawType(), returnType.getRawType(),
					context );
		}

		private <P, T> ServiceMethod<P, T> newServiceMethod( Method service,
				Class<P> parameterType, Class<T> returnType, DependencyResolver context ) {
			return new LazyServiceMethod<P, T>(
					TypeReflector.newInstance( service.getDeclaringClass() ), service,
					parameterType, returnType, context );
		}

		private <P, T> Method resolveServiceMethod( Type<P> parameterType, Type<T> returnType ) {
			for ( Class<?> st : serviceTypes ) {
				for ( Method sm : st.getDeclaredMethods() ) {
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

		private synchronized void resolveServiceTypes( DependencyResolver context ) {
			if ( serviceTypes == null ) {
				Dependency<Class[]> serviceClassesDependency = dependency(
						raw( Class[].class ).parametized( Object.class ).parametizedAsLowerBounds() ).named(
						Name.prefixed( SERVICE_NAME_PREFIX ) );
				serviceTypes = context.resolve( serviceClassesDependency );
			}
		}

	}

	private static class ServiceSupplier
			implements Supplier<ServiceMethod<?, ?>> {

		@Override
		public ServiceMethod<?, ?> supply( Dependency<? super ServiceMethod<?, ?>> dependency,
				DependencyResolver context ) {
			ServiceProvider serviceResolver = context.resolve( dependency( ServiceProvider.class ) );
			Type<?>[] parameters = dependency.getType().getParameters();
			return serviceResolver.provide( parameters[0], parameters[1], context );
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
				method.setAccessible( true );
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
