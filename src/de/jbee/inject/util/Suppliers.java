package de.jbee.inject.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import de.jbee.inject.Dependency;
import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Instance;
import de.jbee.inject.Supplier;
import de.jbee.inject.Type;

public class Suppliers {

	public static final Supplier<Provider<?>> PROVIDER_BRIDGE = new ProviderSupplier();
	public static final Supplier<List<?>> LIST_BRIDGE = new ArrayToListBridgeSupplier();
	public static final Supplier<Set<?>> SET_BRIDGE = new ArrayToSetBridgeSupplier();
	public static final Supplier<Logger> LOGGER = new LoggerSupplier();

	public static <T> Supplier<T> asSupplier( Provider<T> provider ) {
		return new ProviderAsSupplier<T>( provider );
	}

	public static <T> Supplier<Provider<T>> constant( Provider<T> provider ) {
		return new ConstantSupplier<Provider<T>>( provider );
	}

	public static <T> Supplier<T> constant( T constant ) {
		return new ConstantSupplier<T>( constant );
	}

	public static <T> Supplier<T> supplier( Class<? extends Supplier<? extends T>> type ) {
		return new IndirectSupplier<T>( type );
	}

	public static <T> Supplier<T> type( Class<T> type ) {
		return type( Type.raw( type ) );
	}

	public static <T> Supplier<T> type( Type<T> type ) {
		return instance( Instance.anyOf( type ) );
	}

	public static <T> Supplier<T> instance( Instance<T> instance ) {
		return new InstanceSupplier<T>( instance );
	}

	public static <E> Supplier<E[]> elements( Class<E[]> arrayType, Supplier<? extends E>[] elements ) {
		return new ElementsSupplier<E>( arrayType, elements );
	}

	public static <T> Supplier<T> costructor( Constructor<T> constructor ) {
		return new ConstructorSupplier<T>( constructor );
	}

	private static abstract class ArrayBridgeSupplier<T>
			implements Supplier<T> {

		ArrayBridgeSupplier() {
			// make visible
		}

		@Override
		public final T supply( Dependency<? super T> dependency, DependencyResolver context ) {
			Type<?> elementType = dependency.getType().getParameters()[0];
			return bridge( supplyArray( dependency.anyTyped( elementType.getArrayType() ), context ) );
		}

		private <E> E[] supplyArray( Dependency<E[]> elementType, DependencyResolver resolver ) {
			return resolver.resolve( elementType );
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

	private static final class ConstantSupplier<T>
			implements Supplier<T> {

		private final T instance;

		ConstantSupplier( T instance ) {
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
	private static final class ElementsSupplier<E>
			implements Supplier<E[]> {

		private final Class<E[]> arrayType;
		private final Supplier<? extends E>[] elements;

		ElementsSupplier( Class<E[]> arrayType, Supplier<? extends E>[] elements ) {
			super();
			this.arrayType = arrayType;
			this.elements = elements;
		}

		@Override
		public E[] supply( Dependency<? super E[]> dependency, DependencyResolver context ) {
			E[] res = (E[]) Array.newInstance( arrayType.getComponentType(), elements.length );
			int i = 0;
			final Dependency<E> elementDependency = (Dependency<E>) dependency.typed( Type.raw(
					arrayType ).getElementType() );
			for ( Supplier<? extends E> e : elements ) {
				res[i++] = e.supply( elementDependency, context );
			}
			return res;
		}

	}

	private static final class IndirectSupplier<T>
			implements Supplier<T> {

		private final Class<? extends Supplier<? extends T>> type;

		IndirectSupplier( Class<? extends Supplier<? extends T>> type ) {
			super();
			this.type = type;
		}

		@Override
		public T supply( Dependency<? super T> dependency, DependencyResolver context ) {
			final Supplier<? extends T> supplier = context.resolve( dependency.anyTyped( type ) );
			return supplier.supply( dependency, context );
		}
	}

	private static final class InstanceSupplier<T>
			implements Supplier<T> {

		private final Instance<? extends T> instance;

		InstanceSupplier( Instance<? extends T> instance ) {
			super();
			this.instance = instance;
		}

		@Override
		public T supply( Dependency<? super T> dependency, DependencyResolver context ) {
			return context.resolve( dependency.instanced( instance ) );
		}

		@Override
		public String toString() {
			return instance.toString();
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
			return provider.provide();
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
		public T provide() {
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
			System.arraycopy( types, 0, values, 0, types.length );
			return values;
		}

		// in a validate we have to check that the arguments 

		@Override
		public T supply( Dependency<? super T> dependency, DependencyResolver context ) {
			Class<?>[] rawTypes = constructor.getParameterTypes();
			Object[] resolvedArgs = new Object[rawTypes.length];
			for ( int i = 0; i < rawTypes.length; i++ ) {
				if ( args[i] instanceof Type<?> && rawTypes[i] != Type.class ) {
					//OPEN annotations from constructor could be transformed into names for the arguments ?!
					//TODO add/merge target from argument dependency
					Dependency<?> argDependency = dependency.anyTyped( (Type<?>) args[i] );
					resolvedArgs[i] = context.resolve( argDependency ); //FIXME change to Type so dep is constructed using retype 
				} else {
					resolvedArgs[i] = args[i];
				}
			}
			try {
				constructor.setAccessible( true ); //TODO just do it once
				return constructor.newInstance( resolvedArgs );
			} catch ( Exception e ) {
				throw new RuntimeException( e );
			}
		}
	}

	private static class LoggerSupplier
			implements Supplier<Logger> {

		LoggerSupplier() {
			// make visible
		}

		@Override
		public Logger supply( Dependency<? super Logger> dependency, DependencyResolver context ) {
			return Logger.getLogger( dependency.target( 1 ).getType().getRawType().getCanonicalName() );
		}

	}
}
