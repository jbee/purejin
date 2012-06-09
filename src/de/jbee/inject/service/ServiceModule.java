package de.jbee.inject.service;

import static de.jbee.inject.Name.named;
import static de.jbee.inject.Source.source;
import static de.jbee.inject.Type.parameterTypes;
import static de.jbee.inject.Type.returnType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import de.jbee.inject.Bindings;
import de.jbee.inject.Dependency;
import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Module;
import de.jbee.inject.Name;
import de.jbee.inject.Scoped;
import de.jbee.inject.SimpleBinder;
import de.jbee.inject.Supplier;
import de.jbee.inject.Type;
import de.jbee.inject.util.Binder;
import de.jbee.inject.util.Binder.RootBinder;

/**
 * When binding {@link Service}s this {@link Module} can be extended.
 * 
 * It provides service-related build methods.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public abstract class ServiceModule
		implements Module {

	private static final String SERVICE_NAME_PREFIX = "Service-";

	protected final void bindServices( Class<?> service ) {
		String name = SERVICE_NAME_PREFIX + service.getCanonicalName();
		binder.multibind( named( name ), Class.class ).to( service );
	}

	private RootBinder binder;

	@Override
	public final void configure( Bindings instructor ) {
		if ( binder != null ) {
			throw new IllegalStateException( "Reentrance not allowed!" );
		}
		binder = Binder.create( instructor, source( getClass() ) );

		new ServiceCoreModule().configure( instructor );
		//TODO extends ServiceCoreModule
		configure();
	}

	protected abstract void configure();

	static class ServiceCoreModule
			implements Module {

		@Override
		public void configure( Bindings binder ) {
			SimpleBinder bb = new SimpleBinder( binder, source( getClass() ), Scoped.DEFAULT );
			bb.wildcardBind( Service.class, new ServiceSupplier() );
		}

	}

	static class ServiceSupplier
			implements Supplier<Service<?, ?>> {

		private Class<?>[] serviceTypes;

		@Override
		public Service<?, ?> supply( Dependency<? super Service<?, ?>> dependency,
				DependencyResolver context ) {
			if ( serviceTypes == null ) {
				resolveServiceTypes( context );
			}
			Method service = resolveServiceMethod( dependency );
			Type<?>[] parameters = dependency.getType().getParameters();
			return newServiceMethod( service, parameters[0].getRawType(),
					parameters[1].getRawType(), context );
		}

		private <P, T> Service<P, T> newServiceMethod( Method service, Class<P> parameterType,
				Class<T> returnType, DependencyResolver context ) {
			try {
				Constructor<?> constructor = service.getDeclaringClass().getDeclaredConstructor(
						new Class<?>[0] );
				constructor.setAccessible( true );
				return new ServiceMethod<P, T>( constructor.newInstance(), service, parameterType,
						returnType, context );
			} catch ( Exception e ) {
				e.printStackTrace();
				return null;
			}
		}

		private <P, T> Method resolveServiceMethod( Dependency<? super Service<P, T>> dependency ) {
			Type<?> paramType = dependency.getType().getParameters()[0];
			Type<?> returnType = dependency.getType().getParameters()[1];
			for ( Class<?> st : serviceTypes ) {
				for ( Method sm : st.getDeclaredMethods() ) {
					Type<?> rt = returnType( sm );
					if ( rt.equalTo( returnType ) ) {
						for ( Type<?> pt : parameterTypes( sm ) ) {
							if ( pt.equalTo( paramType ) ) {
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
				Dependency<Class[]> serviceClassesDependency = Dependency.dependency(
						Type.raw( Class[].class ).parametized( Object.class ).parametizedAsLowerBounds() ).named(
						Name.prefixed( SERVICE_NAME_PREFIX ) );
				serviceTypes = context.resolve( serviceClassesDependency );
			}
		}

	}

	static class ServiceMethod<P, T>
			implements Service<P, T> {

		private final Object object;
		private final Method method;
		private final Class<P> parameterType;
		private final Class<T> returnType;
		private final DependencyResolver context;
		private final Type<?>[] parameterTypes;

		ServiceMethod( Object object, Method service, Class<P> parameterType, Class<T> returnType,
				DependencyResolver context ) {
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
					args[i] = context.resolve( Dependency.dependency( paramType ) );
				}
			}
			return args;
		}

	}
}
