package de.jbee.silk;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class Suppliers {

	static <T> Supplier<T> valueFromProvider( Provider<T> provider ) {
		return new ProviderAsSupplier<T>( provider );
	}

	static <T> Supplier<Provider<T>> instance( Provider<T> provider ) {
		return new InstanceSupplier<Provider<T>>( provider );
	}

	static <T> Supplier<T> instance( T instance ) {
		return new InstanceSupplier<T>( instance );
	}

	static <T> Supplier<T> type( ClassType<T> type ) {
		return new TypeSupplier<T>( type );
	}

	static final class InstanceSupplier<T>
			implements Supplier<T> {

		private final T instance;

		InstanceSupplier( T instance ) {
			super();
			this.instance = instance;
		}

		@Override
		public T supply( Dependency<T> dependency, DependencyResolver resolver ) {
			return instance;
		}

	}

	static final class ArraySupplier<T>
			implements Supplier<T[]> {

		private final Class<T[]> type;
		private final Supplier<? extends T>[] elements;

		ArraySupplier( Class<T[]> type, Supplier<? extends T>[] elements ) {
			super();
			this.type = type;
			this.elements = elements;
		}

		@Override
		public T[] supply( Dependency<T[]> dependency, DependencyResolver resolver ) {
			T[] res = (T[]) Array.newInstance( type.getComponentType(), elements.length );
			int i = 0;
			for ( Supplier<? extends T> e : elements ) {
				//TODO The element type is the dependency
				res[i++] = e.supply( null, resolver );
			}
			return res;
		}

	}

	static final class TypeSupplier<T>
			implements Supplier<T> {

		private final ClassType<T> type;

		TypeSupplier( ClassType<T> type ) {
			super();
			this.type = type;
		}

		@Override
		public T supply( Dependency<T> dependency, DependencyResolver resolver ) {
			// TODO ? add more information from the dependency ? 
			return resolver.resolve( References.<T> type( type ) );
		}

	}

	static final class ProviderAsSupplier<T>
			implements Supplier<T> {

		private final Provider<T> provider;

		ProviderAsSupplier( Provider<T> provider ) {
			super();
			this.provider = provider;
		}

		@Override
		public T supply( Dependency<T> dependency, DependencyResolver resolver ) {
			return provider.yield();
		}

	}

	static final class ProviderSupplier
			implements Supplier<Provider<?>> {

		@Override
		public Provider<?> supply( Dependency<Provider<?>> dependency, DependencyResolver resolver ) {
			ParameterizedType type = dependency.getParameterizedType();
			Type provided = type.getActualTypeArguments()[0];
			// TODO ? add more information from the dependency ? 
			return new LazyDependencyProvider<Object>( References.type( provided ), resolver );
		}

	}

	static final class LazyDependencyProvider<T>
			implements Provider<T> {

		private final Dependency<T> dependency;
		private final DependencyResolver resolver;

		LazyDependencyProvider( Dependency<T> dependency, DependencyResolver resolver ) {
			super();
			this.dependency = dependency;
			this.resolver = resolver;
		}

		@Override
		public T yield() {
			return resolver.resolve( dependency );
		}

	}

	static final class FactorySupplier<T>
			implements Supplier<T> {

		@Override
		public T supply( Dependency<T> dependency, DependencyResolver resolver ) {
			// TODO Auto-generated method stub
			return null;
		}

		// some kind of factory method 
	}

	static final class ConstructorSupplier<T>
			implements Supplier<T> {

		private final Constructor<T> constructor;
		private final Object[] args;

		ConstructorSupplier( Constructor<T> constructor ) {
			this( constructor, instances( constructor ) );
		}

		private static <T> Object[] instances( Constructor<T> constructor ) {
			Type[] types = constructor.getGenericParameterTypes();
			Object[] values = new Object[types.length];
			for ( int i = 0; i < types.length; i++ ) {
				values[i] = References.type( types[i] );
			}
			return values;
		}

		ConstructorSupplier( Constructor<T> constructor, Object[] args ) {
			super();
			this.constructor = constructor;
			this.args = args;
		}

		// in a validate we have to check that the arguments 

		@Override
		public T supply( Dependency<T> dependency, DependencyResolver resolver ) {
			Type[] types = constructor.getGenericParameterTypes();
			Class<?>[] rawTypes = constructor.getParameterTypes();
			Annotation[][] annotations = constructor.getParameterAnnotations();
			Object[] dependencies = new Object[types.length];
			for ( int i = 0; i < types.length; i++ ) {
				if ( args[i] instanceof Dependency<?>
						&& !Dependency.class.isAssignableFrom( rawTypes[i] ) ) {
					Dependency<?> argDependency = (Dependency<?>) args[i]; //OPEN what about the dependency given ?
					dependencies[i] = resolver.resolve( argDependency );
				} else {
					dependencies[i] = args[i];
				}
			}
			try {
				return constructor.newInstance( dependencies );
			} catch ( Exception e ) {
				throw new RuntimeException( e );
			}
		}
	}
}
