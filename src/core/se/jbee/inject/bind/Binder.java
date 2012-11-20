/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.util.SuppliedBy.constant;
import static se.jbee.inject.util.SuppliedBy.parametrizedInstance;
import static se.jbee.inject.util.SuppliedBy.provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import se.jbee.inject.DeclarationType;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Packages;
import se.jbee.inject.Parameter;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Source;
import se.jbee.inject.Supplier;
import se.jbee.inject.Target;
import se.jbee.inject.Type;
import se.jbee.inject.util.Factory;
import se.jbee.inject.util.Provider;
import se.jbee.inject.util.SuppliedBy;

/**
 * The default implementation of the {@link BasicBinder} that provides a lot of utility methods to
 * improve readability and keep binding compact.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public class Binder
		implements BasicBinder {

	private static class AutobindBindings
			implements Bindings {

		private final Bindings delegate;

		AutobindBindings( Bindings delegate ) {
			super();
			this.delegate = delegate;
		}

		@Override
		public <T> void add( Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
				Source source ) {
			delegate.add( resource, supplier, scope, source );
			Type<T> type = resource.getType();
			for ( Type<? super T> supertype : type.supertypes() ) {
				// Object is of cause a superclass of everything but not indented when doing auto-binds
				if ( supertype.getRawType() != Object.class ) {
					delegate.add( resource.typed( supertype ), supplier, scope, source );
				}
			}
		}
	}

	public static Bindings autobinding( Bindings delegate ) {
		return new AutobindBindings( delegate );
	}

	public static RootBinder create( Bindings bindings, ConstructionStrategy strategy,
			Source source, Scope scope ) {
		return new RootBinder( bindings, strategy, source, scope );
	}

	final Bindings bindings;
	final ConstructionStrategy strategy;
	final Source source;
	final Scope scope;
	final Target target;

	Binder( Bindings bindings, ConstructionStrategy strategy, Source source, Scope scope,
			Target target ) {
		super();
		this.bindings = bindings;
		this.strategy = strategy;
		this.source = source;
		this.scope = scope;
		this.target = target;
	}

	public <T> TypedBinder<T> starbind( Class<T> type ) {
		return bind( Instance.anyOf( Type.raw( type ) ) );
	}

	@Override
	public <T> TypedBinder<T> bind( Instance<T> instance ) {
		return new TypedBinder<T>( this, instance );
	}

	public <T> TypedBinder<T> bind( Name name, Class<T> type ) {
		return bind( name, Type.raw( type ) );
	}

	public <T> TypedBinder<T> bind( Name name, Type<T> type ) {
		return bind( instance( name, type ) );
	}

	public <T> TypedBinder<T> bind( Type<T> type ) {
		return bind( defaultInstanceOf( type ) );
	}

	public <T> TypedBinder<T> bind( Class<T> type ) {
		return bind( Type.raw( type ) );
	}

	public void construct( Class<?> type ) {
		construct( ( defaultInstanceOf( raw( type ) ) ) );
	}

	public void construct( Name name, Class<?> type ) {
		construct( instance( name, raw( type ) ) );
	}

	public void construct( Instance<?> instance ) {
		bind( instance ).toConstructor();
	}

	public <E> TypedElementBinder<E> bind( Class<E[]> type ) {
		return new TypedElementBinder<E>( this, defaultInstanceOf( raw( type ) ) );
	}

	public <T> TypedBinder<T> autobind( Type<T> type ) {
		return into( autobinding( bindings ) ).bind( type );
	}

	public <T> TypedBinder<T> autobind( Class<T> type ) {
		return autobind( Type.raw( type ) );
	}

	public <T> TypedBinder<T> multibind( Instance<T> instance ) {
		return with( source.typed( DeclarationType.MULTI ) ).bind( instance );
	}

	public <T> TypedBinder<T> multibind( Type<T> type ) {
		return multibind( defaultInstanceOf( type ) );
	}

	public <T> TypedBinder<T> multibind( Class<T> type ) {
		return multibind( Type.raw( type ) );
	}

	public <T> TypedBinder<T> multibind( Name name, Class<T> type ) {
		return multibind( instance( name, Type.raw( type ) ) );
	}

	public <T> TypedBinder<T> multibind( Name name, Type<T> type ) {
		return multibind( instance( name, type ) );
	}

	public <E extends Enum<E> & Extension<E, ? super T>, T> void extend( Class<E> extension,
			Class<? extends T> type ) {
		multibind( Extend.extensionName( extension, type ), Class.class ).to( type );
		implicitBindToConstructor( type );
	}

	public <E extends Enum<E> & Extension<E, ? super T>, T> void extend( E extension,
			Class<? extends T> type ) {
		multibind( Extend.extensionName( extension, type ), Class.class ).to( type );
		implicitBindToConstructor( type );
	}

	protected final <T> void bind( Resource<T> resource, Supplier<? extends T> supplier ) {
		bindings.add( resource, supplier, scope, source );
	}

	protected final <I> void implicitBindToConstructor( Class<I> impl ) {
		implicitBindToConstructor( defaultInstanceOf( raw( impl ) ) );
	}

	protected final <I> void implicitBindToConstructor( Instance<I> instance ) {
		Class<I> impl = instance.getType().getRawType();
		if ( notConstructable( impl ) ) {
			return;
		}
		Constructor<I> constructor = strategy.constructorFor( impl );
		if ( constructor != null ) {
			implicit().with( Target.ANY ).bind( instance ).to( constructor );
		}
	}

	protected final boolean notConstructable( Class<?> type ) {
		return type.isInterface() || type.isEnum() || type.isPrimitive() || type.isArray()
				|| Modifier.isAbstract( type.getModifiers() ) || type == String.class
				|| Number.class.isAssignableFrom( type ) || type == Boolean.class
				|| type == Void.class || type == void.class;
	}

	protected final Binder implicit() {
		return with( source.typed( DeclarationType.IMPLICIT ) );
	}

	protected final Binder multi() {
		return with( source.typed( DeclarationType.MULTI ) );
	}

	protected Binder with( Source source ) {
		return new Binder( bindings, strategy, source, scope, target );
	}

	protected Binder with( Target target ) {
		return new Binder( bindings, strategy, source, scope, target );
	}

	protected Binder into( Bindings bindings ) {
		return new Binder( bindings, strategy, source, scope, target );
	}

	/**
	 * This kind of bindings actually re-map the []-type so that the automatic behavior of returning
	 * all known instances of the element type will no longer be used whenever the bind made
	 * applies.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 * 
	 */
	public static class TypedElementBinder<E>
			extends TypedBinder<E[]> {

		TypedElementBinder( Binder binder, Instance<E[]> instance ) {
			super( binder.multi(), instance );
		}

		public void to( Supplier<? extends E>[] elements ) {
			to( SuppliedBy.elements( getType().getRawType(), elements ) );
		}

		@SuppressWarnings ( "unchecked" )
		public void to( Supplier<? extends E> supplier1, Supplier<? extends E> supplier2 ) {
			to( (Supplier<? extends E>[]) new Supplier<?>[] { supplier1, supplier2 } );
		}

		@SuppressWarnings ( "unchecked" )
		public void to( Supplier<? extends E> supplier1, Supplier<? extends E> supplier2,
				Supplier<? extends E> supplier3 ) {
			to( (Supplier<? extends E>[]) new Supplier<?>[] { supplier1, supplier2, supplier3 } );
		}

		public void toElements( E constant1, E constant2 ) {
			to( constant( constant1 ), constant( constant2 ) );
		}

		public void toElements( Class<? extends E> impl1, Class<? extends E> impl2 ) {
			to( supply( impl1 ), supply( impl2 ) );
		}

		public void toElements( Class<? extends E> impl1, Class<? extends E> impl2,
				Class<? extends E> impl3 ) {
			to( supply( impl1 ), supply( impl2 ), supply( impl3 ) );
		}

		@SuppressWarnings ( "unchecked" )
		public void toElements( Class<? extends E> impl1, Class<? extends E> impl2,
				Class<? extends E> impl3, Class<? extends E> impl4 ) {
			to( (Supplier<? extends E>[]) new Supplier<?>[] { supply( impl1 ), supply( impl2 ),
					supply( impl3 ), supply( impl4 ) } );
		}

		@SuppressWarnings ( "unchecked" )
		public void toElements( Class<? extends E>... impls ) {
			Supplier<? extends E>[] suppliers = (Supplier<? extends E>[]) new Supplier<?>[impls.length];
			for ( int i = 0; i < impls.length; i++ ) {
				suppliers[i] = supply( impls[i] );
			}
			to( suppliers );
		}

		// and so on.... will avoid generic array warning here 
	}

	public static class RootBinder
			extends ScopedBinder
			implements RootBasicBinder {

		RootBinder( Bindings bindings, ConstructionStrategy strategy, Source source, Scope scope ) {
			super( bindings, strategy, source, scope );
		}

		@Override
		public ScopedBinder per( Scope scope ) {
			return new ScopedBinder( bindings, strategy, source, scope );
		}

		@Override
		protected RootBinder with( Source source ) {
			return new RootBinder( bindings, strategy, source, scope );
		}

		@Override
		protected RootBinder into( Bindings bindings ) {
			return new RootBinder( bindings, strategy, source, scope );
		}

		protected RootBinder using( ConstructionStrategy strategy ) {
			return new RootBinder( bindings, strategy, source, scope );
		}

		protected RootBinder asDefault() {
			return with( source.typed( DeclarationType.DEFAULT ) );
		}
	}

	public static class ScopedBinder
			extends TargetedBinder
			implements ScopedBasicBinder {

		ScopedBinder( Bindings bindings, ConstructionStrategy strategy, Source source, Scope scope ) {
			super( bindings, strategy, source, scope, Target.ANY );
		}

		public TargetedBinder injectingInto( Class<?> target ) {
			return injectingInto( raw( target ) );
		}

		public TargetedBinder injectingInto( Type<?> target ) {
			return injectingInto( defaultInstanceOf( target ) );
		}

		public TargetedBinder injectingInto( Name name, Class<?> type ) {
			return injectingInto( name, raw( type ) );
		}

		public TargetedBinder injectingInto( Name name, Type<?> type ) {
			return injectingInto( Instance.instance( name, type ) );
		}

		@Override
		public TargetedBinder injectingInto( Instance<?> target ) {
			return new TargetedBinder( bindings, strategy, source, scope, Target.targeting( target ) );
		}

	}

	public static class TargetedBinder
			extends Binder
			implements TargetedBasicBinder {

		TargetedBinder( Bindings bindings, ConstructionStrategy strategy, Source source,
				Scope scope, Target target ) {
			super( bindings, strategy, source, scope, target );
		}

		public TargetedBinder within( Instance<?> parent ) {
			return new TargetedBinder( bindings, strategy, source, scope, target.within( parent ) );
		}

		public TargetedBinder within( Name name, Class<?> parent ) {
			return within( instance( name, raw( parent ) ) );
		}

		public TargetedBinder within( Name name, Type<?> parent ) {
			return within( instance( name, parent ) );
		}

		public TargetedBinder within( Class<?> parent ) {
			return within( raw( parent ) );
		}

		public TargetedBinder within( Type<?> parent ) {
			return within( anyOf( parent ) );
		}

		@Override
		public Binder in( Packages packages ) {
			return with( target.in( packages ) );
		}

		public Binder inPackageOf( Class<?> type ) {
			return with( target.inPackageOf( type ) );
		}

		public Binder inSubPackagesOf( Class<?> type ) {
			return with( target.inSubPackagesOf( type ) );
		}

		public Binder inPackageAndSubPackagesOf( Class<?> type ) {
			return with( target.inPackageAndSubPackagesOf( type ) );
		}
	}

	public static class TypedBinder<T>
			implements BasicBinder.TypedBasicBinder<T> {

		/**
		 * The binder instance who's {@link RichBasicBinder#assemble(Instance)} method had been
		 * called to get to this {@link TypedBasicBinder}.
		 */
		private final Binder binder;
		private final Resource<T> resource;

		TypedBinder( Binder binder, Instance<T> instance ) {
			this( binder, new Resource<T>( instance, binder.target ) );
		}

		TypedBinder( Binder binder, Resource<T> resource ) {
			super();
			this.binder = binder;
			this.resource = resource;
		}

		protected final Type<T> getType() {
			return resource.getType();
		}

		@Override
		public void to( Supplier<? extends T> supplier ) {
			binder.bind( resource, supplier );
		}

		public void to( Factory<? extends T> factory ) {
			to( SuppliedBy.factory( factory ) );
		}

		public void to( Constructor<? extends T> constructor ) {
			to( SuppliedBy.costructor( constructor ) );
		}

		public void to( Constructor<? extends T> constructor, Parameter<?>... parameters ) {
			to( SuppliedBy.costructor( constructor, parameters ) );
		}

		public void toConstructor( Class<? extends T> impl, Parameter<?>... parameters ) {
			if ( binder.notConstructable( impl ) ) {
				throw new IllegalArgumentException( "Not a constructable type: " + impl );
			}
			to( SuppliedBy.costructor( binder.strategy.constructorFor( impl ), parameters ) );
		}

		public void toConstructor( Parameter<?>... parameters ) {
			toConstructor( resource.getType().getRawType(), parameters );
		}

		public void to( T constant ) {
			toConstant( constant );
		}

		/**
		 * This is to do multi-binds in the same module. The {@link Binder#multibind(Class)} methods
		 * are use when a module just does one bind but that is meant to co-exist with others from
		 * other modules.
		 */
		public void to( T constant1, T constant2 ) {
			multi().toConstant( constant1 ).toConstant( constant2 );
		}

		public <I extends Supplier<? extends T>> void toSupplier( Class<I> impl ) {
			to( SuppliedBy.reference( impl ) );
			implicitBindToConstructor( defaultInstanceOf( raw( impl ) ) );
		}

		public void to( Provider<? extends T> provider ) {
			to( provider( provider ) );
			implicitProvider( provider );
		}

		@SuppressWarnings ( "unchecked" )
		private <P> void implicitProvider( Provider<P> provider ) {
			Type<Provider<P>> providerType = (Type<Provider<P>>) Type.supertype( Provider.class,
					raw( provider.getClass() ) );
			binder.implicit().bind( defaultInstanceOf( providerType ) ).to(
					SuppliedBy.constant( provider ) );
		}

		public void toConstructor() {
			to( binder.strategy.constructorFor( resource.getType().getRawType() ) );
		}

		public <I extends T> void to( Class<I> impl ) {
			to( Instance.anyOf( raw( impl ) ) );
		}

		public <I extends T> void toParametrized( Class<I> impl ) {
			to( parametrizedInstance( Instance.anyOf( raw( impl ) ) ) );
		}

		public <I extends T> void to( Name name, Type<I> type ) {
			to( instance( name, type ) );
		}

		public <I extends T> void to( Name name, Class<I> type ) {
			to( instance( name, raw( type ) ) );
		}

		public <I extends T> void to( Instance<I> instance ) {
			to( supply( instance ) );
		}

		public void to( Method factory, Parameter<?>... parameters ) {
			to( SuppliedBy.method( resource.getType(), factory, parameters ) );
		}

		public void toMethod( Class<?> implementor, Parameter<?>... parameters ) {
			to( binder.strategy.factoryFor( resource.getType(), resource.getName(), implementor ),
					parameters );
			implicitBindToConstructor( Instance.anyOf( raw( implementor ) ) );
		}

		<I> Supplier<I> supply( Class<I> impl ) {
			return supply( Instance.anyOf( raw( impl ) ) );
		}

		<I> Supplier<I> supply( Instance<I> instance ) {
			if ( !resource.getInstance().equalTo( instance ) ) {
				implicitBindToConstructor( instance );
				return SuppliedBy.instance( instance );
			}
			if ( instance.getType().getRawType().isInterface() ) {
				throw new IllegalArgumentException( "Interface type linked in a loop: "
						+ resource.getInstance() + " > " + instance );
			}
			return SuppliedBy.costructor( binder.strategy.constructorFor( instance.getType().getRawType() ) );
		}

		<I> void implicitBindToConstructor( Instance<I> instance ) {
			binder.implicitBindToConstructor( instance );
		}

		protected final TypedBinder<T> multi() {
			return new TypedBinder<T>( binder.multi(), resource );
		}

		private TypedBinder<T> toConstant( T constant ) {
			to( SuppliedBy.constant( constant ) );
			return this;
		}

	}
}