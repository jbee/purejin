package de.jbee.inject.service;

import static de.jbee.inject.Name.named;
import static de.jbee.inject.Source.source;
import static de.jbee.inject.Type.parameterTypes;

import java.lang.reflect.Method;

import de.jbee.inject.BasicBinder;
import de.jbee.inject.Binder;
import de.jbee.inject.Dependency;
import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Module;
import de.jbee.inject.Scoped;
import de.jbee.inject.Supplier;
import de.jbee.inject.Type;
import de.jbee.inject.util.RichBinder;
import de.jbee.inject.util.RichBinder.RichRootBinder;

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

	protected final void bindService( Class<?> service ) {
		String name = SERVICE_NAME_PREFIX + service.getCanonicalName();
		root.multibind( named( name ), Class.class ).to( service );
	}

	private RichRootBinder root;

	@Override
	public final void configure( Binder binder ) {
		if ( root != null ) {
			throw new IllegalStateException( "Reentrance not allowed!" );
		}
		root = RichBinder.root( binder, source( getClass() ) );
		//TODO extends ServiceCoreModule
		configure();
	}

	protected abstract void configure();

	static class ServiceCoreModule
			implements Module {

		@Override
		public void configure( Binder binder ) {
			BasicBinder bb = new BasicBinder( binder, source( getClass() ), Scoped.DEFAULT );
			bb.wildcardBind( Service.class, new ServiceSupplier() );
		}

	}

	static class ServiceSupplier
			implements Supplier<Service<?, ?>> {

		@Override
		public Service<?, ?> supply( Dependency<? super Service<?, ?>> dependency,
				DependencyResolver context ) {
			// TODO find method matching dependency and return new ServiceMethod
			return null;
		}

	}

	static class ServiceMethod<P, T>
			implements Service<P, T> {

		private final Class<?> declaredIn;
		private final Method method;
		private final Class<P> parameterType;
		private final Class<T> returnType;
		private final DependencyResolver context;
		private final Type<?>[] parameterTypes;

		ServiceMethod( Class<?> declaredIn, Method method, Class<P> parameterType,
				Class<T> returnType, DependencyResolver context ) {
			super();
			this.declaredIn = declaredIn;
			this.method = method;
			this.parameterType = parameterType;
			this.returnType = returnType;
			this.context = context;
			this.parameterTypes = parameterTypes( method );
		}

		@Override
		public T invoke( P params ) {
			Object[] args = actualArguments( params );
			try {
				return returnType.cast( method.invoke( declaredIn, args ) );
			} catch ( Exception e ) {
				throw new RuntimeException( "Failed to invoke service", e );
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
					context.resolve( Dependency.dependency( paramType ) );
				}
			}
			return args;
		}

	}
}
