package de.jbee.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Suppliers {

	public static final Supplier<Provider<?>> PROVIDER = new ProviderSupplier();

	public static final Supplier<List<?>> LIST_BRIDGE = new ArrayToListBridgeSupplier();

	public static <T> Supplier<T> adapt( Provider<T> provider ) {
		return new ProviderAdaptedSupplier<T>( provider );
	}

	public static <T> Supplier<Provider<T>> instance( Provider<T> provider ) {
		return new InstanceSupplier<Provider<T>>( provider );
	}

	public static <T> Supplier<T> instance( T instance ) {
		return new InstanceSupplier<T>( instance );
	}

	public static <T> Supplier<T> type( Type<T> type ) {
		return new TypeSupplier<T>( type );
	}

	public static <T> Injectable<T> asInjectable( Supplier<T> supplier,
			DependencyResolver context ) {
		return new InjectableSupplier<T>( supplier, context );
	}

	/**
	 * Shows how support for {@link List}s and such works.
	 * 
	 * Basically we just resolve the array of the element type (generic of the list). Arrays itself
	 * have build in support that will (if not redefined by a more precise binding) return all known
	 * 
	 * TODO extract a abstract {@link Supplier} for 1 argument generic type's
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 * 
	 */
	private static final class ArrayToListBridgeSupplier
			implements Supplier<List<?>> {

		ArrayToListBridgeSupplier() {
			//make visible
		}

		@Override
		public List<?> supply( Dependency<List<?>> dependency, DependencyResolver context ) {
			Type<?> elementType = dependency.getType().getParameters()[0];
			return new ArrayList<Object>( Arrays.asList( supplyArray( elementType.getRawType(),
					context ) ) );
		}

		private <E> E[] supplyArray( Class<E> elementType, DependencyResolver resolver ) {
			Object proto = Array.newInstance( elementType, 0 );
			return (E[]) resolver.resolve( Dependency.dependency( Type.type( proto.getClass(),
					proto.getClass() ) ) );
		}

	}

	private static final class InstanceSupplier<T>
			implements Supplier<T> {

		private final T instance;

		InstanceSupplier( T instance ) {
			super();
			this.instance = instance;
		}

		@Override
		public T supply( Dependency<T> dependency, DependencyResolver context ) {
			return instance;
		}

	}

	private static final class ArraySupplier<T>
			implements Supplier<T[]> {

		private final Class<T[]> type;
		private final Supplier<? extends T>[] elements;

		ArraySupplier( Class<T[]> type, Supplier<? extends T>[] elements ) {
			super();
			this.type = type;
			this.elements = elements;
		}

		@Override
		public T[] supply( Dependency<T[]> dependency, DependencyResolver context ) {
			T[] res = (T[]) Array.newInstance( type.getComponentType(), elements.length );
			int i = 0;
			for ( Supplier<? extends T> e : elements ) {
				//TODO The element type is the dependency
				res[i++] = e.supply( null, context );
			}
			return res;
		}

	}

	private static final class TypeSupplier<T>
			implements Supplier<T> {

		private final Type<T> type;

		TypeSupplier( Type<T> type ) {
			super();
			this.type = type;
		}

		@Override
		public T supply( Dependency<T> dependency, DependencyResolver context ) {
			// TODO ? add more information from the dependency ? 
			Dependency<T> typeDependency = null; //FIXME merge type and dependency 
			return context.resolve( typeDependency );
		}

	}

	private static final class ProviderAdaptedSupplier<T>
			implements Supplier<T> {

		private final Provider<T> provider;

		ProviderAdaptedSupplier( Provider<T> provider ) {
			super();
			this.provider = provider;
		}

		@Override
		public T supply( Dependency<T> dependency, DependencyResolver context ) {
			return provider.yield();
		}

	}

	private static final class ProviderSupplier
			implements Supplier<Provider<?>> {

		ProviderSupplier() {
			//make visible
		}

		@Override
		public Provider<?> supply( Dependency<Provider<?>> dependency, DependencyResolver context ) {
			Type<Provider<?>> type = dependency.getType();
			Type<?> provided = type.getParameters()[0];
			// TODO ? add more information from the dependency ? 
			Dependency<Object> providedDependency = null; //FIXME merge dependency and provided
			return new DynamicProvider<Object>( providedDependency, context );
		}

	}

	private static final class DynamicProvider<T>
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

	/**
	 * Adapter to a simpler API that will not need any {@link DependencyResolver} to supply it's
	 * value in any case.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 * 
	 */
	static final class FactorySupplier<T>
			implements Supplier<T> {

		@Override
		public T supply( Dependency<T> dependency, DependencyResolver context ) {
			// TODO Auto-generated method stub
			return null;
		}

		// some kind of factory method working on the type/dependency alone, not interested in the context
	}

	static final class ConstructorSupplier<T>
			implements Supplier<T> {

		private final Constructor<T> constructor;
		private final Object[] args;

		ConstructorSupplier( Constructor<T> constructor ) {
			this( constructor, instances( constructor ) );
		}

		private static <T> Object[] instances( Constructor<T> constructor ) {
			Class<?>[] rawTypes = constructor.getParameterTypes();
			java.lang.reflect.Type[] types = constructor.getGenericParameterTypes();
			Object[] values = new Object[types.length];
			for ( int i = 0; i < types.length; i++ ) {
				values[i] = Dependency.dependency( Type.type( rawTypes[i], types[i] ) );
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
		public T supply( Dependency<T> dependency, DependencyResolver context ) {
			java.lang.reflect.Type[] types = constructor.getGenericParameterTypes();
			Class<?>[] rawTypes = constructor.getParameterTypes();
			Annotation[][] annotations = constructor.getParameterAnnotations();
			Object[] dependencies = new Object[types.length];
			for ( int i = 0; i < types.length; i++ ) {
				if ( args[i] instanceof Dependency<?>
						&& !Dependency.class.isAssignableFrom( rawTypes[i] ) ) {
					Dependency<?> argDependency = (Dependency<?>) args[i]; //OPEN what about the dependency given ?
					dependencies[i] = context.resolve( argDependency );
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

	private static class InjectableSupplier<T>
			implements Injectable<T> {

		private final Supplier<T> supplier;
		private final DependencyResolver context;

		InjectableSupplier( Supplier<T> supplier, DependencyResolver context ) {
			super();
			this.supplier = supplier;
			this.context = context;
		}

		@Override
		public T instanceFor( Injection<T> injection ) {
			return supplier.supply( injection.getDependency(), context );
		}
	}
}
