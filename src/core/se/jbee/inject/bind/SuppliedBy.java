/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.parameterTypes;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.bind.Parameterize.parameterizations;
import static se.jbee.inject.util.ToString.describe;

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
import se.jbee.inject.DIRuntimeException.NoSuchResourceException;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Parameter;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Invoke;
import se.jbee.inject.util.Factory;
import se.jbee.inject.util.Metaclass;
import se.jbee.inject.util.Parameterization;
import se.jbee.inject.util.Provider;

/**
 * Utility as a factory to create different kinds of {@link Supplier}s.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class SuppliedBy {

	private static final Parameterization<?>[] NO_PARAMS = new Parameterization<?>[0];
	static final Object[] NO_ARGS = new Object[0];

	private static final Supplier<?> REQUIRED = new RequiredSupplier<Object>();

	public static final Supplier<Provider<?>> PROVIDER_BRIDGE = new ProviderSupplier();
	public static final Supplier<List<?>> LIST_BRIDGE = new ArrayToListBridgeSupplier();
	public static final Supplier<Set<?>> SET_BRIDGE = new ArrayToSetBridgeSupplier();
	public static final Factory<Logger> LOGGER = new LoggerFactory();

	@SuppressWarnings ( "unchecked" )
	public static <T> Supplier<T> required() {
		return (Supplier<T>) REQUIRED;
	}

	public static <T> Supplier<T> constant( T constant ) {
		return new ConstantSupplier<T>( constant );
	}

	public static <T> Supplier<T> reference( Class<? extends Supplier<? extends T>> type ) {
		return new ReferenceSupplier<T>( type );
	}

	public static <E> Supplier<E[]> references( Class<E[]> arrayType,
			Supplier<? extends E>[] elements ) {
		return new ReferencesSupplier<E>( arrayType, elements );
	}

	public static <T> Supplier<T> instance( Instance<T> instance ) {
		return new InstanceSupplier<T>( instance );
	}

	public static <T> Supplier<T> dependency( Dependency<T> dependency ) {
		return new DependencySupplier<T>( dependency );
	}

	public static <T> Supplier<T> parametrizedInstance( Instance<T> instance ) {
		return new ParametrizedInstanceSupplier<T>( instance );
	}

	public static <T> Supplier<T> method( Type<T> returnType, Object instance, Method factory,
			Parameter<?>... parameters ) {
		final Type<?> actualReturnType = Type.returnType( factory );
		if ( !actualReturnType.isAssignableTo( returnType ) ) {
			throw new IllegalArgumentException( "The factory methods methods return type `"
					+ actualReturnType + "` is not assignable to: " + returnType );
		}
		if ( instance != null && factory.getDeclaringClass() != instance.getClass() ) {
			throw new IllegalArgumentException(
					"The factory method and the instance it is invoked on have to be the same class." );
		}
		Parameterization<?>[] params = parameterizations( parameterTypes( factory ), parameters );
		return new MethodSupplier<T>( returnType, factory, instance, params );
	}

	public static <T> Supplier<T> costructor( Constructor<T> constructor,
			Parameter<?>... parameters ) {
		final Class<?>[] params = constructor.getParameterTypes();
		if ( params.length == 0 ) {
			return new ConstructorSupplier<T>( constructor, NO_PARAMS );
		}
		return new ConstructorSupplier<T>( constructor, parameterizations(
				parameterTypes( constructor ), parameters ) );
	}

	public static <T> Supplier<T> factory( Factory<T> factory ) {
		return new FactorySupplier<T>( factory );
	}

	public static <T, C> Supplier<T> configuration( Type<T> type, Configuring<C> configuration ) {
		return new ConfigurationDependentSupplier<T, C>( type, configuration );
	}

	public static <T> Provider<T> lazyProvider( Dependency<T> dependency, Injector context ) {
		return new LazyProvider<T>( dependency, context );
	}

	public static <T> Object[] resolve( Dependency<? super T> parent, Injector injector,
			Parameterization<?>[] params ) {
		Object[] args = new Object[params.length];
		for ( int i = 0; i < params.length; i++ ) {
			args[i] = resolve( parent, injector, params[i] );
		}
		return args;
	}

	public static <T> T resolve( Dependency<?> parent, Injector injector, Parameterization<T> param ) {
		return param.supply( parent.instanced( anyOf( param.getType() ) ), injector );
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
			return bridge( supplyArray( dependency.typed( elementType.getArrayType() ), injector ) );
		}

		private static <E> E[] supplyArray( Dependency<E[]> elementType, Injector resolver ) {
			return resolver.resolve( elementType );
		}

		abstract <E> T bridge( E[] elements );
	}

	/**
	 * This is a indirection that resolves a {@link Type} dependent on another current
	 * {@link Configuring} value. This can be understand as a dynamic <i>name</i> switch so that a
	 * call is resolved to different named instances.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	private static final class ConfigurationDependentSupplier<T, C>
			implements Supplier<T> {

		private final Type<T> type;
		private final Configuring<C> configuration;

		ConfigurationDependentSupplier( Type<T> type, Configuring<C> configuration ) {
			super();
			this.type = type;
			this.configuration = configuration;
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			final C value = injector.resolve( dependency.instanced( configuration.getInstance() ) );
			return supply( dependency, injector, value );
		}

		private T supply( Dependency<? super T> dependency, Injector injector, final C value ) {
			final Instance<T> current = Instance.instance( configuration.name( value ), type );
			try {
				return injector.resolve( dependency.instanced( current ) );
			} catch ( NoSuchResourceException e ) {
				return supply( dependency, injector, null );
			}
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

	private static final class DependencySupplier<T>
			implements Supplier<T> {

		private final Dependency<T> dependency;

		DependencySupplier( Dependency<T> dependency ) {
			super();
			this.dependency = dependency;
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			return injector.resolve( this.dependency );
		}

		@Override
		public String toString() {
			return dependency.toString();
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
	private static final class ReferencesSupplier<E>
			implements Supplier<E[]> {

		private final Class<E[]> arrayType;
		private final Supplier<? extends E>[] elements;

		ReferencesSupplier( Class<E[]> arrayType, Supplier<? extends E>[] elements ) {
			super();
			this.arrayType = arrayType;
			this.elements = elements;
		}

		@SuppressWarnings ( "unchecked" )
		@Override
		public E[] supply( Dependency<? super E[]> dependency, Injector injector ) {
			E[] res = (E[]) Array.newInstance( arrayType.getComponentType(), elements.length );
			int i = 0;
			final Dependency<E> elementDependency = (Dependency<E>) dependency.typed( raw(
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
			return lazyProvider( providedType.uninject().ignoredExpiry(), injector );
		}

	}

	private static final class LazyProvider<T>
			implements Provider<T> {

		private final Dependency<T> dependency;
		private final Injector injector;

		LazyProvider( Dependency<T> dependency, Injector injector ) {
			super();
			this.dependency = dependency;
			this.injector = injector;
		}

		@Override
		public T provide() {
			return injector.resolve( dependency );
		}

		@Override
		public String toString() {
			return describe( "provides", dependency );
		}
	}

	/**
	 * Adapter to a simpler API that will not need any {@link Injector} to supply it's value in any
	 * case.
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

		@Override
		public String toString() {
			return describe( "factory", factory );
		}

	}

	private static final class ConstructorSupplier<T>
			implements Supplier<T> {

		private final Constructor<T> constructor;
		private final Parameterization<?>[] params;

		ConstructorSupplier( Constructor<T> constructor, Parameterization<?>[] params ) {
			super();
			this.constructor = Metaclass.accessible( constructor );
			this.params = params;
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			final Object[] args = params.length == 0
				? NO_ARGS
				: resolve( dependency, injector, params );
			return Invoke.constructor( constructor, args );
		}

		@Override
		public String toString() {
			return describe( constructor, params );
		}
	}

	private static final class MethodSupplier<T>
			implements Supplier<T> {

		private final Type<T> returnType;
		private final Method factory;
		private final Object instance;
		private final Parameterization<?>[] params;
		private final boolean instanceMethod;

		MethodSupplier( Type<T> returnType, Method factory, Object instance,
				Parameterization<?>[] params ) {
			super();
			this.returnType = returnType;
			this.factory = Metaclass.accessible( factory );
			this.instance = instance;
			this.params = params;
			this.instanceMethod = !Modifier.isStatic( factory.getModifiers() );
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			Object owner = instance;
			if ( instanceMethod && owner == null ) {
				owner = injector.resolve( Dependency.dependency( factory.getDeclaringClass() ) );
			}
			final Object[] args = params.length == 0
				? NO_ARGS
				: resolve( dependency, injector, params );
			return returnType.getRawType().cast( Invoke.method( factory, owner, args ) );
		}

		@Override
		public String toString() {
			return describe( factory, params );
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

		@Override
		public String toString() {
			return describe( "required" );
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
