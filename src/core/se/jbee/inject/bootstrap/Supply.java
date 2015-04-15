/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.parameterTypes;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.bootstrap.BoundParameter.bind;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import se.jbee.inject.Array;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Injectron;
import se.jbee.inject.Instance;
import se.jbee.inject.Parameter;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.container.Factory;
import se.jbee.inject.container.Provider;

/**
 * Utility as a factory to create different kinds of {@link Supplier}s.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Supply {

	private static final Object[] NO_ARGS = new Object[0];
	static final Type<Object[]> OBJECT_ARRAY = raw(Object[].class);

	public static final Supplier<Provider<?>> PROVIDER_BRIDGE = new ProviderSupplier();
	public static final Supplier<List<?>> LIST_BRIDGE = new ArrayToListBridgeSupplier();
	public static final Supplier<Set<?>> SET_BRIDGE = new ArrayToSetBridgeSupplier();
	public static final Factory<Logger> LOGGER = new LoggerFactory();

	private static final Supplier<?> REQUIRED = new RequiredSupplier<Object>();

	/**
	 * A {@link Supplier} used as fall-back. Should a required resource not be
	 * provided it is still bound to this supplier that will throw an exception
	 * should it ever be called.
	 */
	@SuppressWarnings ( "unchecked" )
	public static <T> Supplier<T> required() {
		return (Supplier<T>) REQUIRED;
	}

	public static <T> Supplier<T> constant( T constant ) {
		return new ConstantSupplier<T>( constant );
	}

	public static <T> Supplier<T> reference( Class<? extends Supplier<? extends T>> type ) {
		return new BridgeSupplier<T>( type );
	}

	public static <E> Supplier<E[]> elements( Type<E[]> arrayType, Parameter<? extends E>[] elements ) {
		return new PredefinedArraySupplier<E>( arrayType, BoundParameter.bind( elements ) );
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

	public static <T> Supplier<T> method( BoundMethod<T> factory ) {
		return new MethodSupplier<T>( factory, parameters( parameterTypes( factory.factory ), factory.parameters ));
	}

	public static <T> Supplier<T> costructor( BoundConstructor<T> constructor ) {
		return new ConstructorSupplier<T>( 
				Metaclass.accessible( constructor.constructor ), 
				parameters( parameterTypes( constructor.constructor ), constructor.parameters ));
	}
	
	public static Supplier<Object[]> parameters(Type<?>[] types, Parameter<?>... parameters) {
		return new BufferedParameterSupplier(bind(types, parameters));
	}

	public static <T> Supplier<T> factory( Factory<T> factory ) {
		return new FactorySupplier<T>( factory );
	}

	public static <T> Provider<T> lazyProvider( Dependency<T> dependency, Injector context ) {
		return new LazyProvider<T>( dependency, context );
	}

	public static <T> Object[] resolveParameters( Dependency<? super T> parent, Injector injector, BoundParameter<?>[] params ) {
		if ( params.length == 0 ) {
			return NO_ARGS;
		}
		Object[] args = new Object[params.length];
		for ( int i = 0; i < params.length; i++ ) {
			args[i] = resolveParameter( parent, injector, params[i] );
		}
		return args;
	}

	public static <T> T resolveParameter( Dependency<?> parent, Injector injector, BoundParameter<T> param ) {
		return param.supply( parent.instanced( anyOf( param.type() ) ), injector );
	}

	private Supply() {
		throw new UnsupportedOperationException( "util" );
	}

	public static abstract class ArrayBridgeSupplier<T>
			implements Supplier<T> {

		ArrayBridgeSupplier() {
			// make visible
		}

		@Override
		public final T supply( Dependency<? super T> dependency, Injector injector ) {
			Type<?> elementType = dependency.type().parameter( 0 );
			return bridge( supplyArray( dependency.typed( elementType.addArrayDimension() ), injector ) );
		}

		private static <E> E[] supplyArray( Dependency<E[]> elementType, Injector resolver ) {
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
			return Arrays.asList( elements );
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
			return describe( "supplies", dependency );
		}
	}

	private static final class ConstantSupplier<T>
			implements Supplier<T> {

		private final T constant;

		ConstantSupplier( T constant ) {
			super();
			this.constant = constant;
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			return constant;
		}

		@Override
		public String toString() {
			return describe( "supplies", constant );
		}

	}

	/**
	 * A {@link Supplier} uses multiple different separate suppliers to provide the elements of a
	 * array of the supplied type.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	@Deprecated // should use new args
	private static final class PredefinedArraySupplier<E>
			implements Supplier<E[]> {

		private final Type<E[]> arrayType;
		private final BoundParameter<? extends E>[] elements;

		PredefinedArraySupplier( Type<E[]> arrayType, BoundParameter<? extends E>[] elements ) {
			super();
			this.arrayType = arrayType;
			this.elements = elements;
		}

		@SuppressWarnings ( "unchecked" )
		@Override
		public E[] supply( Dependency<? super E[]> dependency, Injector injector ) {
			final Type<?> elementType = arrayType.baseType();
			final E[] res = (E[]) Array.newInstance( elementType.rawType, elements.length );
			final Dependency<E> elementDependency = (Dependency<E>) dependency.typed( elementType );
			int i = 0;
			for ( BoundParameter<? extends E> e : elements ) {
				res[i++] = e.supply( elementDependency, injector );
			}
			return res;
		}

		@Override
		public String toString() {
			return describe( "supplies", elements );
		}
	}

	private static final class BridgeSupplier<T>
			implements Supplier<T> {

		private final Class<? extends Supplier<? extends T>> type;

		BridgeSupplier( Class<? extends Supplier<? extends T>> type ) {
			super();
			this.type = type;
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			final Supplier<? extends T> supplier = injector.resolve( dependency.instanced(anyOf(type)));
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
			Type<? super T> type = dependency.type();
			Instance<? extends T> parametrized = instance.typed( instance.type().parametized(
					type.parameters() ).upperBound( dependency.type().isUpperBound() ) );
			return injector.resolve( dependency.instanced( parametrized ) );
		}

		@Override
		public String toString() {
			return describe( "supplies", instance );
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
			return describe( "supplies", instance );
		}
	}

	private static final class ProviderSupplier
			implements Supplier<Provider<?>> {

		ProviderSupplier() {
			//make visible
		}

		@Override
		public Provider<?> supply( Dependency<? super Provider<?>> dependency, Injector injector ) {
			return lazyProvider( dependency.onTypeParameter().uninject().ignoredExpiry(), injector );
		}

		@Override
		public String toString() {
			return describe( "supplies", Provider.class );
		}
	}

	private static final class LazyProvider<T>
			implements Provider<T> {

		private final Dependency<T> dependency;
		private final Injectron<? extends T> injectron;
		
		@SuppressWarnings("unchecked")
		LazyProvider( Dependency<T> dependency, Injector injector ) {
			super();
			this.dependency = dependency;
			this.injectron = injector.resolve(dependency.typed(raw( Injectron.class ).parametized(dependency.type())));
		}

		@Override
		public T provide() {
			return injectron.instanceFor(dependency);
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
			return factory.fabricate( dependency.instance, dependency.target( 1 ) );
		}

		@Override
		public String toString() {
			return describe( "supplies", factory );
		}

	}
	
	static final class BufferedParameterSupplier implements Supplier<Object[]> {

		private final BoundParameter<?>[] params;
		
		public BufferedParameterSupplier(BoundParameter<?>[] params) {
			super();
			this.params = params;
		}

		@Override
		public Object[] supply(Dependency<? super Object[]> dependency,	Injector injector) throws UnresolvableDependency {
			return resolveParameters( dependency, injector, params );
		}
		
	}

	private static final class ConstructorSupplier<T>
			implements Supplier<T> {

		private final Constructor<T> constructor;
		private final Supplier<Object[]> args;

		ConstructorSupplier( Constructor<T> constructor, Supplier<Object[]> args ) {
			this.constructor = constructor;
			this.args=args;
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			return Invoke.constructor( constructor, args.supply(dependency.typed(OBJECT_ARRAY), injector) );
		}

		@Override
		public String toString() {
			return describe( constructor, args );
		}
	}

	private static final class MethodSupplier<T>
			implements Supplier<T> {

		private final BoundMethod<T> method;
		private final Supplier<Object[]> args;

		MethodSupplier( BoundMethod<T> method, Supplier<Object[]> args ) {
			super();
			this.method = method;
			this.args = args;
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			Object owner = method.instance;
			if ( method.isInstanceMethod && owner == null ) {
				owner = injector.resolve( Dependency.dependency( method.factory.getDeclaringClass() ) );
			}
			return method.returnType.rawType.cast(
					Invoke.method( method.factory, owner, args.supply(dependency.typed(OBJECT_ARRAY), injector)) );
		}

		@Override
		public String toString() {
			return describe( method.factory, args );
		}
	}

	private static class RequiredSupplier<T>
			implements Supplier<T> {

		RequiredSupplier() {
			// make visible
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			throw required(dependency);
		}
		
		@SuppressWarnings("unchecked")
		private static <T> NoResourceForDependency required(Dependency<T> dependency) {
			return new NoResourceForDependency(dependency, (Injectron<T>[])new Injectron<?>[0], "Should never be called!" );
		}

		@Override
		public String toString() {
			return Supply.describe( "required" );
		}
	}

	private static class LoggerFactory
			implements Factory<Logger> {

		LoggerFactory() {
			// make visible
		}

		@Override
		public <P> Logger fabricate( Instance<? super Logger> created, Instance<P> receiver ) {
			return Logger.getLogger( receiver.type().rawType.getCanonicalName() );
		}

	}

	public static String describe( Object behaviour ) {
		return "<" + behaviour + ">";
	}

	public static String describe( Object behaviour, Object variant ) {
		return "<" + behaviour + ":" + variant + ">";
	}

	public static String describe( Object behaviour, Object[] variants ) {
		return describe( behaviour, Arrays.toString( variants ) );
	}
	
	static class Foo {
		
		private final Injector injector;
		private final Injectron<?>[] argumentInjectrons;
		private final Type<?>[] parameterTypes;
		private final Object[] argumentTemplate;

		Foo(Injector injector, Type<?>[] types) {
			this.injector = injector;
			this.parameterTypes = types;
			this.argumentInjectrons = argumentInjectrons();
			this.argumentTemplate = argumentTemplate();
		}
		
		private Object[] argumentTemplate() {
			Object[] template = new Object[parameterTypes.length];
			for ( int i = 0; i < template.length; i++ ) {
				Injectron<?> injectron = argumentInjectrons[i];
				if ( injectron != null && injectron.info().expiry.isNever() ) {
					template[i] = instance( injectron, Dependency.dependency( parameterTypes[i] ) );
				}
			}
			return template;
		}

		private Injectron<?>[] argumentInjectrons() {
			Injectron<?>[] res = new Injectron<?>[parameterTypes.length];
			for ( int i = 0; i < res.length; i++ ) {
				Type<?> paramType = parameterTypes[i];
				res[i] = injector.resolve( Dependency.dependency( raw( Injectron.class ).parametized( paramType ) ) );
			}
			return res;
		}

		private Object[] actualArguments() {
			Object[] args = argumentTemplate.clone();
			for ( int i = 0; i < args.length; i++ ) {
				Type<?> paramType = parameterTypes[i];
				args[i] = instance( argumentInjectrons[i], Dependency.dependency( paramType ) );
			}
			return args;
		}

		@SuppressWarnings ( "unchecked" )
		private static <I> I instance( Injectron<I> injectron, Dependency<?> dependency ) {
			return injectron.instanceFor( (Dependency<? super I>) dependency );
		}		
	}
}
