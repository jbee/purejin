package de.jbee.silk;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Suppliers {

	public static <T> Supplier<T> valueFromProvider( Provider<T> provider ) {
		return new ProviderSupplier<T>( provider );
	}

	public static <T> Supplier<Provider<T>> instance( Provider<T> provider ) {
		return new InstanceSupplier<Provider<T>>( provider );
	}

	public static <T> Supplier<T> instance( T instance ) {
		return new InstanceSupplier<T>( instance );
	}

	public static <T> Supplier<T> type( ClassType<T> type ) {
		return new TypeSupplier<T>( type );
	}

	/**
	 * Shows how support for {@link List}s and such works.
	 * 
	 * Basically we just resolve the array of the element type (generic of the list). Arrays itself
	 * have build in support that will (if not redefined by a more precise binding) return all known
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 * 
	 */
	static final class ListArrayBridgeSupplier
			implements Supplier<List<?>> {

		@Override
		public List<?> supply( Dependency<List<?>> dependency, DependencyResolver resolver ) {
			Type elementType = dependency.getParameterizedType().getActualTypeArguments()[0];
			return new ArrayList<Object>(
					Arrays.asList( supply( (Class<?>) elementType, resolver ) ) );
		}

		private <E> E[] supply( Class<E> elementType, DependencyResolver resolver ) {
			Object proto = Array.newInstance( elementType, 0 );
			return (E[]) resolver.resolve( References.type( proto.getClass() ) );
		}

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

	static final class ProviderSupplier<T>
			implements Supplier<T> {

		private final Provider<T> provider;

		ProviderSupplier( Provider<T> provider ) {
			super();
			this.provider = provider;
		}

		@Override
		public T supply( Dependency<T> dependency, DependencyResolver resolver ) {
			return provider.yield();
		}

	}

	static final class ProviderBrideSupplier
			implements Supplier<Provider<?>> {

		@Override
		public Provider<?> supply( Dependency<Provider<?>> dependency, DependencyResolver resolver ) {
			ParameterizedType type = dependency.getParameterizedType();
			Type provided = type.getActualTypeArguments()[0];
			// TODO ? add more information from the dependency ? 
			return new DynamicProvider<Object>( References.type( provided ), resolver );
		}

	}

	static final class DynamicProvider<T>
			implements Provider<T> {

		private final Dependency<T> dependency;
		private final DependencyResolver resolver;

		DynamicProvider( Dependency<T> dependency, DependencyResolver resolver ) {
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
