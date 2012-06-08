package de.jbee.inject;

import static de.jbee.inject.Dependency.dependency;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Suppliers {

	public static final Supplier<Provider<?>> PROVIDER = new ProviderSupplier();
	public static final Supplier<List<?>> LIST_BRIDGE = new ArrayToListBridgeSupplier();
	public static final Supplier<Set<?>> SET_BRIDGE = new ArrayToSetBridgeSupplier();

	public static <T> Supplier<T> asSupplier( Provider<T> provider ) {
		return new ProviderAsSupplier<T>( provider );
	}

	public static <T> Supplier<Provider<T>> instance( Provider<T> provider ) {
		return new InstanceSupplier<Provider<T>>( provider );
	}

	public static <T> Supplier<T> instance( T instance ) {
		return new InstanceSupplier<T>( instance );
	}

	public static <T> Supplier<T> link( Class<? extends Supplier<? extends T>> type ) {
		return new LinkSupplier<T>( type );
	}

	public static <T> Supplier<T> type( Class<T> type ) {
		return type( Type.raw( type ) );
	}

	public static <T> Supplier<T> type( Type<T> type ) {
		return new TypeSupplier<T>( type );
	}

	public static <T> Supplier<T> costructor( Constructor<T> constructor ) {
		return new ConstructorSupplier<T>( constructor );
	}

	public static <T> Injectable<T> asInjectable( Supplier<? extends T> supplier,
			DependencyResolver context ) {
		return new SupplierToInjectable<T>( supplier, context );
	}

	private static abstract class ArrayBridgeSupplier<T>
			implements Supplier<T> {

		ArrayBridgeSupplier() {
			// make visible
		}

		@Override
		public final T supply( Dependency<? super T> dependency, DependencyResolver context ) {
			Type<?> elementType = dependency.getType().getParameters()[0];
			return bridge( supplyArray( elementType, context ) );
		}

		private <E> E[] supplyArray( Type<E> elementType, DependencyResolver resolver ) {
			return resolver.resolve( Dependency.dependency( elementType.getArrayType() ) );
		}

		abstract <E> T bridge( E[] elements );
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
	private static final class ArrayToListBridgeSupplier
			extends ArrayBridgeSupplier<List<?>> {

		ArrayToListBridgeSupplier() {
			//make visible
		}

		@Override
		<E> List<E> bridge( E[] elements ) {
			return new ArrayList<E>( Arrays.asList( elements ) );
		}

	}

	private static final class ArrayToSetBridgeSupplier
			extends ArrayBridgeSupplier<Set<?>> {

		ArrayToSetBridgeSupplier() {
			// make visible
		}

		@Override
		<E> Set<E> bridge( E[] elements ) {
			return new HashSet<E>( Arrays.asList( elements ) );
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
		public T supply( Dependency<? super T> dependency, DependencyResolver context ) {
			return instance;
		}

		@Override
		public String toString() {
			return instance.toString();
		}

	}

	/**
	 * A {@link Supplier} uses multiple different separate suppliers to provide the elements of a
	 * array of the supplied type.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	private static final class MultiSupplier<T>
			implements Supplier<T[]> {

		private final Class<T[]> type;
		private final Supplier<? extends T>[] elements;

		MultiSupplier( Class<T[]> type, Supplier<? extends T>[] elements ) {
			super();
			this.type = type;
			this.elements = elements;
		}

		@Override
		public T[] supply( Dependency<? super T[]> dependency, DependencyResolver context ) {
			T[] res = (T[]) Array.newInstance( type.getComponentType(), elements.length );
			int i = 0;
			final Dependency<T> elementDependency = (Dependency<T>) dependency.typed( Type.raw(
					type ).getElementType() );
			for ( Supplier<? extends T> e : elements ) {
				res[i++] = e.supply( elementDependency, context );
			}
			return res;
		}

	}

	private static final class LinkSupplier<T>
			implements Supplier<T> {

		private final Class<? extends Supplier<? extends T>> type;

		LinkSupplier( Class<? extends Supplier<? extends T>> type ) {
			super();
			this.type = type;
		}

		@Override
		public T supply( Dependency<? super T> dependency, DependencyResolver context ) {
			final Supplier<? extends T> supplier = context.resolve( dependency.typed( Type.raw( type ) ) );
			return supplier.supply( dependency, context );
		}
	}

	private static final class TypeSupplier<T>
			implements Supplier<T> {

		private final Type<? extends T> type;

		TypeSupplier( Type<? extends T> type ) {
			super();
			this.type = type;
		}

		@Override
		public T supply( Dependency<? super T> dependency, DependencyResolver context ) {
			//OPEN is it correct to keep name from dependency ?
			return context.resolve( dependency.typed( type ) );
		}

		@Override
		public String toString() {
			return type.toString();
		}
	}

	private static final class ProviderAsSupplier<T>
			implements Supplier<T> {

		private final Provider<T> provider;

		ProviderAsSupplier( Provider<T> provider ) {
			super();
			this.provider = provider;
		}

		@Override
		public T supply( Dependency<? super T> dependency, DependencyResolver context ) {
			return provider.yield();
		}

	}

	private static final class ProviderSupplier
			implements Supplier<Provider<?>> {

		ProviderSupplier() {
			//make visible
		}

		@Override
		public Provider<?> supply( Dependency<? super Provider<?>> dependency,
				DependencyResolver context ) {
			return newProvider( dependency.onTypeParameter(), context );
		}

		private <T> Provider<T> newProvider( Dependency<T> dependency, DependencyResolver context ) {
			return new LazyResolvedDependencyProvider<T>( dependency, context );
		}
	}

	private static final class LazyResolvedDependencyProvider<T>
			implements Provider<T> {

		private final Dependency<T> dependency;
		private final DependencyResolver resolver;

		LazyResolvedDependencyProvider( Dependency<T> dependency, DependencyResolver resolver ) {
			super();
			this.dependency = dependency;
			this.resolver = resolver;
		}

		@Override
		public T yield() {
			return resolver.resolve( dependency );
		}

		@Override
		public String toString() {
			return "provides<" + dependency + ">";
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
		public T supply( Dependency<? super T> dependency, DependencyResolver context ) {
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
			this( constructor, dependencies( constructor ) );
		}

		ConstructorSupplier( Constructor<T> constructor, Object[] args ) {
			super();
			this.constructor = constructor;
			this.args = args;
		}

		private static <T> Object[] dependencies( Constructor<T> constructor ) {
			Type<?>[] types = Type.parameterTypes( constructor );
			Object[] values = new Object[types.length];
			for ( int i = 0; i < types.length; i++ ) {
				values[i] = dependency( types[i] );
			}
			return values;
		}

		// in a validate we have to check that the arguments 

		@Override
		public T supply( Dependency<? super T> dependency, DependencyResolver context ) {
			java.lang.reflect.Type[] types = constructor.getGenericParameterTypes();
			Class<?>[] rawTypes = constructor.getParameterTypes();
			Object[] resolvedArgs = new Object[types.length];
			for ( int i = 0; i < types.length; i++ ) {
				if ( args[i] instanceof Dependency<?> && rawTypes[i] != Dependency.class ) {
					//OPEN annotations from constructor could be transformed into names for the arguments ?!
					Dependency<?> argDependency = (Dependency<?>) args[i];
					resolvedArgs[i] = context.resolve( argDependency );
				} else {
					resolvedArgs[i] = args[i];
				}
			}
			try {
				return constructor.newInstance( resolvedArgs );
			} catch ( Exception e ) {
				throw new RuntimeException( e );
			}
		}
	}

	private static class SupplierToInjectable<T>
			implements Injectable<T> {

		private final Supplier<? extends T> supplier;
		private final DependencyResolver context;

		SupplierToInjectable( Supplier<? extends T> supplier, DependencyResolver context ) {
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
