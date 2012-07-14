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
import de.jbee.inject.Hint;
import de.jbee.inject.Instance;
import de.jbee.inject.Supplier;
import de.jbee.inject.Type;
import de.jbee.inject.Typed;

public class SuppliedBy {

	public static final Supplier<Provider<?>> PROVIDER_BRIDGE = new ProviderSupplier();
	public static final Supplier<List<?>> LIST_BRIDGE = new ArrayToListBridgeSupplier();
	public static final Supplier<Set<?>> SET_BRIDGE = new ArrayToSetBridgeSupplier();
	public static final Factory<Logger> LOGGER = new LoggerSupplier();

	public static <T> Supplier<T> provider( Provider<T> provider ) {
		return new ProviderAsSupplier<T>( provider );
	}

	public static <T> Supplier<Provider<T>> constant( Provider<T> provider ) {
		return new ConstantSupplier<Provider<T>>( provider );
	}

	public static <T> Supplier<T> constant( T constant ) {
		return new ConstantSupplier<T>( constant );
	}

	public static <T> Supplier<T> reference( Class<? extends Supplier<? extends T>> type ) {
		return new ReferencingSupplier<T>( type );
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
		final Class<?>[] params = constructor.getParameterTypes();
		if ( params.length == 0 ) {
			return new NoArgsConstructorSupplier<T>( constructor );
		}
		return new ConstructorSupplier<T>( constructor, new Object[params.length] );
	}

	public static <T> Supplier<T> costructor( Constructor<T> constructor, Object... hints ) {
		if ( hints.length == 0 ) {
			return costructor( constructor );
		}
		Type<?>[] types = Type.parameterTypes( constructor );
		Object[] matchingHints = new Object[constructor.getParameterTypes().length];
		for ( Object hint : hints ) {
			int i = 0;
			boolean found = false;
			while ( i < types.length && !found ) {
				final Type<?> paramType = types[i];
				if ( hint instanceof Class<?> ) {
					found = Type.raw( (Class<?>) hint ).isAssignableTo( paramType );
				} else if ( hint instanceof Type<?> ) {
					found = ( (Type<?>) hint ).isAssignableTo( paramType );
				} else if ( hint instanceof Typed<?> ) {
					found = ( (Typed<?>) hint ).getType().isAssignableTo( paramType );
				} else {
					found = Type.raw( hint.getClass() ).isAssignableTo( paramType );
				}
				if ( found ) {
					matchingHints[i] = hint;
				}
				i++;
			}
			if ( !found ) {
				throw new IllegalArgumentException( "Couldn't understand hint: " + hint );
			}
		}
		return new ConstructorSupplier<T>( constructor, matchingHints );
	}

	public static <T> Supplier<T> factory( Factory<T> factory ) {
		return new FactorySupplier<T>( factory );
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

		@SuppressWarnings ( "unchecked" )
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

	private static final class ReferencingSupplier<T>
			implements Supplier<T> {

		private final Class<? extends Supplier<? extends T>> type;

		ReferencingSupplier( Class<? extends Supplier<? extends T>> type ) {
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
	private static final class FactorySupplier<T>
			implements Supplier<T> {

		private final Factory<T> factory;

		FactorySupplier( Factory<T> factory ) {
			super();
			this.factory = factory;
		}

		@Override
		public T supply( Dependency<? super T> dependency, DependencyResolver context ) {
			return factory.produce( dependency.getInstance(), dependency.target( 1 ) );
		}

	}

	/**
	 * A simple version for all the no-args constructors hopefully used in large numbers.
	 */
	private static final class NoArgsConstructorSupplier<T>
			implements Supplier<T> {

		private final Constructor<T> noArgsConstructor;

		NoArgsConstructorSupplier( Constructor<T> noArgsConstructor ) {
			super();
			TypeReflector.makeAccessible( noArgsConstructor );
			this.noArgsConstructor = noArgsConstructor;
		}

		@Override
		public T supply( Dependency<? super T> dependency, DependencyResolver context ) {
			return TypeReflector.construct( noArgsConstructor );
		}

	}

	private static final class ConstructorSupplier<T>
			implements Supplier<T> {

		private final Constructor<T> constructor;
		private final Type<?>[] types;
		private final Object[] hints;

		ConstructorSupplier( Constructor<T> constructor, Object[] hints ) {
			super();
			TypeReflector.makeAccessible( constructor );
			this.constructor = constructor;
			this.hints = hints;
			this.types = Type.parameterTypes( constructor );
			// TODO when all hints are already actual arguments we can directly invoke the constructor with the hints --> maybe we use the no-args constructor for that and make it a fixed args constructor 
		}

		@Override
		public T supply( Dependency<? super T> dependency, DependencyResolver context ) {
			Object[] args = hints.clone();
			for ( int i = 0; i < types.length; i++ ) {
				final Object hint = hints[i];
				final Type<?> type = types[i];
				if ( hint == null ) {
					args[i] = context.resolve( dependency.anyTyped( type ) );
				} else if ( hint instanceof Class<?> ) {
					args[i] = context.resolve( dependency.anyTyped( (Class<?>) hint ) );
				} else if ( hint instanceof Hint ) {
					if ( hint instanceof Instance<?> ) {
						args[i] = context.resolve( dependency.instanced( (Instance<?>) hint ) );
					} else if ( hint instanceof Type<?> ) {
						args[i] = context.resolve( dependency.anyTyped( (Type<?>) hint ) );
					} else if ( hint instanceof Dependency<?> ) {
						args[i] = context.resolve( (Dependency<?>) hint );
					}
				}
			}
			return TypeReflector.construct( constructor, args );
		}

	}

	private static class LoggerSupplier
			implements Factory<Logger> {

		LoggerSupplier() {
			// make visible
		}

		@Override
		public <P> Logger produce( Instance<? super Logger> produced, Instance<P> injected ) {
			return Logger.getLogger( injected.getType().getRawType().getCanonicalName() );
		}

	}
}
