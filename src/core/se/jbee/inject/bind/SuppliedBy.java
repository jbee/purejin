/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.parameterTypes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import se.jbee.inject.Array;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Parameter;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Invoke;
import se.jbee.inject.util.Argument;
import se.jbee.inject.util.Factory;
import se.jbee.inject.util.Metaclass;
import se.jbee.inject.util.Provider;

/**
 * Utility as a factory to create different kinds of {@link Supplier}s.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class SuppliedBy {

	private static final Object[] NO_ARGS = new Object[0];

	private static final Supplier<?> REQUIRED = new RequiredSupplier<Object>();

	public static final Supplier<Provider<?>> PROVIDER_BRIDGE = new ProviderSupplier();
	public static final Supplier<List<?>> LIST_BRIDGE = new ArrayToListBridgeSupplier();
	public static final Supplier<Set<?>> SET_BRIDGE = new ArrayToSetBridgeSupplier();
	public static final Factory<Logger> LOGGER = new LoggerFactory();

	@SuppressWarnings ( "unchecked" )
	public static <T> Supplier<T> required() {
		return (Supplier<T>) REQUIRED;
	}

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
		return new ReferenceSupplier<T>( type );
	}

	public static <T> Supplier<T> instance( Instance<T> instance ) {
		return new InstanceSupplier<T>( instance );
	}

	public static <T> Supplier<T> parametrizedInstance( Instance<T> instance ) {
		return new ParametrizedInstanceSupplier<T>( instance );
	}

	public static <E> Supplier<E[]> elements( Class<E[]> arrayType, Supplier<? extends E>[] elements ) {
		return new ElementsSupplier<E>( arrayType, elements );
	}

	public static <T> Supplier<T> method( Type<T> returnType, Method factory, Object instance,
			Parameter<?>... parameters ) {
		if ( !Type.returnType( factory ).isAssignableTo( returnType ) ) {
			throw new IllegalArgumentException( "The factory methods methods return type `"
					+ Type.returnType( factory ) + "` is not assignable to: " + returnType );
		}
		if ( instance != null && factory.getDeclaringClass() != instance.getClass() ) {
			throw new IllegalArgumentException(
					"The factory method and the instance it is invoked on have to be the same class." );
		}
		Argument<?>[] arguments = Argument.arguments( Type.parameterTypes( factory ), parameters );
		return new FactoryMethodSupplier<T>( returnType, factory, instance, arguments );
	}

	public static <T> Supplier<T> costructor( Constructor<T> constructor,
			Parameter<?>... parameters ) {
		final Class<?>[] params = constructor.getParameterTypes();
		if ( params.length == 0 ) {
			return new StaticConstructorSupplier<T>( constructor, NO_ARGS );
		}
		Argument<?>[] arguments = Argument.arguments( parameterTypes( constructor ), parameters );
		return Argument.allConstants( arguments )
			? new StaticConstructorSupplier<T>( constructor, Argument.constantsFrom( arguments ) )
			: new ConstructorSupplier<T>( constructor, arguments );
	}

	public static <T> Supplier<T> factory( Factory<T> factory ) {
		return new FactorySupplier<T>( factory );
	}

	public static <T, C extends Enum<?>> Supplier<T> configuration( Type<T> type,
			Class<C> configuration ) {
		return new ConfigurationDependentSupplier<T, C>( type, configuration );
	}

	private SuppliedBy() {
		throw new UnsupportedOperationException( "util" );
	}

	public static abstract class ArrayBridgeSupplier<T>
			implements Supplier<T> {

		ArrayBridgeSupplier() {
			// make visible
		}

		@Override
		public final T supply( Dependency<? super T> dependency, Injector injector ) {
			Type<?> elementType = dependency.getType().parameter( 0 );
			return bridge( supplyArray( dependency.anyTyped( elementType.getArrayType() ), injector ) );
		}

		private static <E> E[] supplyArray( Dependency<E[]> elementType, Injector resolver ) {
			return resolver.resolve( elementType );
		}

		abstract <E> T bridge( E[] elements );
	}

	private static final class ConfigurationDependentSupplier<T, C extends Enum<?>>
			implements Supplier<T> {

		private final Type<T> type;
		private final Class<C> configuration;

		ConfigurationDependentSupplier( Type<T> type, Class<C> configuration ) {
			super();
			this.type = type;
			this.configuration = configuration;
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			C value = injector.resolve( dependency.anyTyped( configuration ) );
			return injector.resolve( dependency.instanced( Instance.instance( named( value ), type ) ) );
		}

	}

	/**
	 * Shows how support for {@link List}s and such works.
	 * 
	 * Basically we just resolve the array of the element type (generic of the list). Arrays itself
	 * have build in support that will (if not redefined by a more precise binding) return all known
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
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
		public T supply( Dependency<? super T> dependency, Injector injector ) {
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
	 * @author Jan Bernitt (jan@jbee.se)
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
		public E[] supply( Dependency<? super E[]> dependency, Injector injector ) {
			E[] res = (E[]) Array.newInstance( arrayType.getComponentType(), elements.length );
			int i = 0;
			final Dependency<E> elementDependency = (Dependency<E>) dependency.typed( Type.raw(
					arrayType ).elementType() );
			for ( Supplier<? extends E> e : elements ) {
				res[i++] = e.supply( elementDependency, injector );
			}
			return res;
		}

	}

	private static final class ReferenceSupplier<T>
			implements Supplier<T> {

		private final Class<? extends Supplier<? extends T>> type;

		ReferenceSupplier( Class<? extends Supplier<? extends T>> type ) {
			super();
			this.type = type;
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			final Supplier<? extends T> supplier = injector.resolve( dependency.anyTyped( type ) );
			return supplier.supply( dependency, injector );
		}
	}

	private static final class ParametrizedInstanceSupplier<T>
			implements Supplier<T> {

		private final Instance<? extends T> instance;

		ParametrizedInstanceSupplier( Instance<? extends T> instance ) {
			super();
			this.instance = instance;
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			Type<? super T> type = dependency.getType();
			Instance<? extends T> parametrized = instance.typed( instance.getType().parametized(
					type.getParameters() ).lowerBound( dependency.getType().isLowerBound() ) );
			return injector.resolve( dependency.instanced( parametrized ) );
		}

		@Override
		public String toString() {
			return instance.toString();
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
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			return injector.resolve( dependency.instanced( instance ) );
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
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			return provider.provide();
		}

	}

	private static final class ProviderSupplier
			implements Supplier<Provider<?>> {

		ProviderSupplier() {
			//make visible
		}

		@Override
		public Provider<?> supply( Dependency<? super Provider<?>> dependency, Injector injector ) {
			Dependency<?> providedType = dependency.onTypeParameter();
			if ( !dependency.getName().isDefault() ) {
				providedType = providedType.named( dependency.getName() );
			}
			return newProvider( providedType, injector );
		}

		private static <T> Provider<T> newProvider( Dependency<T> dependency, Injector context ) {
			return new LazyResolvedDependencyProvider<T>( dependency, context );
		}
	}

	private static final class LazyResolvedDependencyProvider<T>
			implements Provider<T> {

		private final Dependency<T> dependency;
		private final Injector injector;

		LazyResolvedDependencyProvider( Dependency<T> dependency, Injector injector ) {
			super();
			this.dependency = dependency.uninject().ignoredExpiry();
			this.injector = injector;
		}

		@Override
		public T provide() {
			return injector.resolve( dependency );
		}

		@Override
		public String toString() {
			return "provides<" + dependency + ">";
		}
	}

	/**
	 * Adapter to a simpler API that will not need any {@link Injector} to supply it's value in any
	 * case.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
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
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			return factory.produce( dependency.getInstance(), dependency.target( 1 ) );
		}

	}

	/**
	 * A simple version for all the constructors where we know all arguments as constants.
	 */
	private static final class StaticConstructorSupplier<T>
			implements Supplier<T> {

		private final Constructor<T> constructor;
		private final Object[] arguments;

		StaticConstructorSupplier( Constructor<T> constructor, Object[] arguments ) {
			super();
			this.constructor = Metaclass.accessible( constructor );
			this.arguments = arguments;
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			return Invoke.constructor( constructor, arguments );
		}

	}

	private static final class ConstructorSupplier<T>
			implements Supplier<T> {

		private final Constructor<T> constructor;
		private final Argument<?>[] arguments;

		ConstructorSupplier( Constructor<T> constructor, Argument<?>[] arguments ) {
			super();
			this.constructor = Metaclass.accessible( constructor );
			this.arguments = arguments;
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			return Invoke.constructor( constructor,
					Argument.resolve( dependency, injector, arguments ) );
		}

	}

	private static final class FactoryMethodSupplier<T>
			implements Supplier<T> {

		private final Type<T> returnType;
		private final Method factory;
		private final Object instance;
		private final Argument<?>[] arguments;
		private final boolean instanceMethod;

		FactoryMethodSupplier( Type<T> returnType, Method factory, Object instance,
				Argument<?>[] arguments ) {
			super();
			this.returnType = returnType;
			this.factory = Metaclass.accessible( factory );
			this.instance = instance;
			this.arguments = arguments;
			this.instanceMethod = !Modifier.isStatic( factory.getModifiers() );
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			Object owner = instance;
			if ( instanceMethod && owner == null ) {
				owner = injector.resolve( dependency( factory.getDeclaringClass() ) );
			}
			final Object[] args = Argument.resolve( dependency, injector, arguments );
			return returnType.getRawType().cast( Invoke.method( factory, owner, args ) );
		}

	}

	private static class RequiredSupplier<T>
			implements Supplier<T> {

		RequiredSupplier() {
			// make visible
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			throw new UnsupportedOperationException( "Should never be called!" );
		}

	}

	private static class LoggerFactory
			implements Factory<Logger> {

		LoggerFactory() {
			// make visible
		}

		@Override
		public <P> Logger produce( Instance<? super Logger> produced, Instance<P> injected ) {
			return Logger.getLogger( injected.getType().getRawType().getCanonicalName() );
		}

	}
}
