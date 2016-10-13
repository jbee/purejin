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

	public static final Supplier<Provider<?>> PROVIDER_BRIDGE = new ProviderSupplier();
	public static final Supplier<List<?>> LIST_BRIDGE = new ArrayToListBridgeSupplier();
	public static final Supplier<Set<?>> SET_BRIDGE = new ArrayToSetBridgeSupplier();
	public static final Factory<Logger> LOGGER = new LoggerFactory();

	private static final Supplier<?> REQUIRED = new RequiredSupplier<>();

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
		return new ConstantSupplier<>( constant );
	}

	public static <T> Supplier<T> reference( Class<? extends Supplier<? extends T>> type ) {
		return new BridgeSupplier<>( type );
	}

	public static <E> Supplier<E[]> elements( Type<E[]> arrayType, Parameter<? extends E>[] elements ) {
		return new PredefinedArraySupplier<>( arrayType, BoundParameter.bind( elements ) );
	}

	public static <T> Supplier<T> instance( Instance<T> instance ) {
		return new InstanceSupplier<>( instance );
	}

	public static <T> Supplier<T> dependency( Dependency<T> dependency ) {
		return new DependencySupplier<>( dependency );
	}

	public static <T> Supplier<T> parametrizedInstance( Instance<T> instance ) {
		return new ParametrizedInstanceSupplier<>( instance );
	}

	public static <T> Supplier<T> method( BoundMethod<T> method ) {
		return new MethodSupplier<>(method, 
				bind( parameterTypes(method.factory), method.parameters ));
	}

	public static <T> Supplier<T> costructor( BoundConstructor<T> constructor ) {
		return new ConstructorSupplier<>( constructor.constructor, 
				bind( parameterTypes(constructor.constructor), constructor.parameters));
	}

	public static <T> Supplier<T> factory( Factory<T> factory ) {
		return new FactorySupplier<>( factory );
	}

	public static <T> Provider<T> lazyProvider( Dependency<T> dependency, Injector injector ) {
		return dependency.type().arrayDimensions() == 1 // no injectrons for results composed within the Injector
				? new LazyDirectProvider<>(dependency, injector) 
				: new LazyProvider<>(dependency, injector);
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
			return new HashSet<>( Arrays.asList( elements ) );
		}

	}

	@Deprecated // should not be needed 
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
	private static final class PredefinedArraySupplier<E> extends WithParameters<E[]> {

		private final Type<E[]> arrayType;
		private final E[] res;

		@SuppressWarnings ( "unchecked" )
		PredefinedArraySupplier( Type<E[]> arrayType, BoundParameter<? extends E>[] elements ) {
			super(elements);
			this.arrayType = arrayType;
			this.res = (E[]) Array.newInstance( arrayType.baseType().rawType, elements.length );
		}

		@Override
		protected void init(Dependency<? super E[]> dependency, Injector injector) { /*NOOP*/ }
		
		@Override
		protected E[] invoke(Object[] args) {
			System.arraycopy(args, 0, res, 0, res.length);
			return res;
		}

		@Override
		public String toString() {
			return describe( "supplies", arrayType );
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

	/**
	 * E.g. used to "forward" Collection<T> to List<T>.
	 */
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
			// Note that this is not "buffered" using Injectrons as it is used to implement the plain resolution
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
	
	private static final class LazyDirectProvider<T> implements Provider<T> {
		
		private final Dependency<T> dependency;
		private final Injector injector;

		LazyDirectProvider(Dependency<T> dependency, Injector injector) {
			this.dependency = dependency;
			this.injector = injector;
		}

		@Override
		public T provide() throws UnresolvableDependency {
			return injector.resolve(dependency);
		}
		
	}

	private static final class LazyProvider<T> implements Provider<T> {

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
	
	private static final class ConstructorSupplier<T> extends WithParameters<T> {

		private final Constructor<T> constructor;

		ConstructorSupplier( Constructor<T> constructor, BoundParameter<?>[] params) {
			super(params);
			this.constructor = constructor;
		}

		@Override
		protected void init(Dependency<? super T> dependency, Injector injector) { /*NOOP*/}

		@Override
		protected T invoke(Object[] args) {
			return Invoke.constructor(constructor, args);
		}

		@Override
		public String toString() {
			return describe( constructor );
		}
	}

	private static final class MethodSupplier<T> extends WithParameters<T> {

		private final BoundMethod<T> method;
		private Object owner;
		private final Class<T> returnType;
	
		MethodSupplier( BoundMethod<T> method, BoundParameter<?>[] parameters ) {
			super(parameters);
			this.method = method;
			this.returnType = method.returnType.rawType;
			this.owner = method.instance;
		}

		@Override
		protected void init(Dependency<? super T> dependency, Injector injector) {
			if ( method.isInstanceMethod && owner == null ) {
				owner = injector.resolve( Dependency.dependency( method.factory.getDeclaringClass() ) );
			}
		}
		
		@Override
		protected T invoke(Object[] args) {
			return returnType.cast(Invoke.method( method.factory, owner, args ));
		}

		@Override
		public String toString() {
			return describe( method.factory );
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
	
	public static abstract class WithParameters<T> implements Supplier<T> {
		
		private final BoundParameter<?>[] params;

		private InjectionSite previous;

		WithParameters(BoundParameter<?>[] params) {
			super();
			this.params = params;

		}
		
		protected abstract void init(Dependency<? super T> dependency, Injector injector);
		
		protected abstract T invoke(Object[] args);
		
		@Override
		public T supply(Dependency<? super T> dependency, Injector injector) throws UnresolvableDependency {
			InjectionSite local = previous; // this is important so previous might work as a simple cache but never causes trouble for this invocation in face of multiple threads calling
			if (local == null) {
				init(dependency, injector);
			}
			if (local == null || !local.site.equalTo(dependency)) {
				local = new InjectionSite(dependency, injector, params);
				previous = local;
			}
			return invoke(local.args(injector));	
		}
	
	}
}
