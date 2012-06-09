package de.jbee.inject.util;

import static de.jbee.inject.Instance.defaultInstanceOf;
import static de.jbee.inject.Instance.instance;
import static de.jbee.inject.Suppliers.asSupplier;
import static de.jbee.inject.Suppliers.instance;
import static de.jbee.inject.Type.instanceType;
import static de.jbee.inject.Type.raw;

import java.lang.reflect.Constructor;

import de.jbee.inject.Availability;
import de.jbee.inject.Bindings;
import de.jbee.inject.Instance;
import de.jbee.inject.Name;
import de.jbee.inject.Provider;
import de.jbee.inject.Resource;
import de.jbee.inject.Scope;
import de.jbee.inject.Scoped;
import de.jbee.inject.Source;
import de.jbee.inject.Supplier;
import de.jbee.inject.Suppliers;
import de.jbee.inject.Type;

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
			for ( Type<? super T> supertype : type.getSupertypes() ) {
				delegate.add( resource.typed( supertype ), supplier, scope, source );
			}
		}
	}

	public static Bindings autobinds( Bindings delegate ) {
		return new AutobindBindings( delegate );
	}

	static class SimpleBindStrategy
			implements BindStrategy {

		@Override
		public <T> Constructor<T> defaultConstructor( Class<T> type ) {
			try {
				return type.getConstructor( new Class<?>[0] );
			} catch ( Exception e ) {
				e.printStackTrace();
				return null;
			}
		}

	}

	public static RootBinder create( Bindings bindings, Source source ) {
		return create( bindings, new SimpleBindStrategy(), source );
	}

	public static RootBinder create( Bindings bindings, BindStrategy strategy, Source source ) {
		return new RootBinder( bindings, strategy, source );
	}

	private final Bindings bindings;
	private final BindStrategy strategy;
	private final Source source;
	private final Scope scope;
	private final Availability availability;

	Binder( Bindings bindings, BindStrategy strategy, Source source, Scope scope,
			Availability availability ) {
		super();
		this.bindings = bindings;
		this.strategy = strategy;
		this.source = source;
		this.scope = scope;
		this.availability = availability;
	}

	@Override
	public <T> TypedBinder<T> bind( Instance<T> instance ) {
		return new TypedBinder<T>( this, instance );
	}

	public <T> TypedBinder<T> bind( Name name, Class<T> type ) {
		return bind( instance( name, Type.raw( type ) ) );
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

	public <E> TypedElementBinder<E> bind( Class<E[]> type ) {
		return new TypedElementBinder<E>( this, Instance.defaultInstanceOf( raw( type ) ) );
	}

	public <T> TypedBinder<T> autobind( Type<T> type ) {
		return with( autobinds( bindings ) ).bind( type );
	}

	public <T> TypedBinder<T> autobind( Class<T> type ) {
		return autobind( Type.raw( type ) );
	}

	public <T> TypedBinder<T> multibind( Instance<T> instance ) {
		return with( source.multi() ).bind( instance );
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

	final Availability availability() {
		return availability;
	}

	final Source source() {
		return source;
	}

	final Scope scope() {
		return scope;
	}

	//OPEN maybe Binder implements bindings and delegates instead of exposing it here
	final Bindings bindings() {
		return bindings;
	}

	final BindStrategy strategy() {
		return strategy;
	}

	final <T> void bindInternal( Resource<T> resource, Supplier<? extends T> supplier ) {
		bindings.add( resource, supplier, scope, source );
	}

	protected final Binder implicit() {
		return with( source.implicit() );
	}

	protected final Binder multi() {
		return with( source.multi() );
	}

	protected final Binder with( Source source ) {
		return new Binder( bindings, strategy, source, scope, availability );
	}

	protected final Binder with( Availability availability ) {
		return new Binder( bindings, strategy, source, scope, availability );
	}

	protected final Binder with( Bindings bindings ) {
		return new Binder( bindings, strategy, source, scope, availability );
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
			to( Suppliers.elements( getType().getRawType(), elements ) );
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

		public void toElements( E instance1, E instance2 ) {
			to( instance( instance1 ), instance( instance2 ) );
		}

		public void toElements( Class<? extends E> elem1, Class<? extends E> elem2 ) {
			to( Suppliers.type( elem1 ), Suppliers.type( elem2 ) );
			bindImplicitToConstructor( elem1, elem2 );
		}

		public void toElements( Class<? extends E> elem1, Class<? extends E> elem2,
				Class<? extends E> elem3 ) {
			to( Suppliers.type( elem1 ), Suppliers.type( elem2 ), Suppliers.type( elem3 ) );
			bindImplicitToConstructor( elem1, elem2, elem3 );
		}

		// and so on.... will avoid generic array warning here 
	}

	public static class RootBinder
			extends ScopedBinder
			implements RootBasicBinder {

		RootBinder( Bindings bindings, BindStrategy strategy, Source source ) {
			super( bindings, strategy, source, Scoped.DEFAULT );
		}

		@Override
		public ScopedBinder in( Scope scope ) {
			return new ScopedBinder( bindings(), strategy(), source(), scope );
		}
	}

	public static class ScopedBinder
			extends TargetedBinder
			implements ScopedBasicBinder {

		ScopedBinder( Bindings bindings, BindStrategy strategy, Source source, Scope scope ) {
			super( bindings, strategy, source, scope );
		}

		// means when the type/instance is created and dependencies are injected into it
		public TargetedBinder injectingInto( Class<?> target ) {
			return injectingInto( defaultInstanceOf( raw( target ) ) );
		}

		@Override
		public TargetedBinder injectingInto( Instance<?> target ) {
			return new TargetedBinder( bindings(), strategy(), source(), scope(), target );
		}

	}

	public static class TargetedBinder
			extends Binder
			implements TargetedBasicBinder {

		TargetedBinder( Bindings bindings, BindStrategy strategy, Source source, Scope scope ) {
			super( bindings, strategy, source, scope, Availability.EVERYWHERE );
		}

		TargetedBinder( Bindings bindings, BindStrategy strategy, Source source, Scope scope,
				Instance<?> target ) {
			super( bindings, strategy, source, scope, Availability.availability( target ) );
		}

		//TODO improve this since from a dependency point of view it is good to localize all binds somehow
		// instead of narrow explicit we could expose explicit and make binds as narrow as possible by default (classic interface to impl binds in same package)

		public Binder inPackageOf( Class<?> packageOf ) {
			return with( availability().within( packageOf.getPackage().getName() ) );
		}

	}

	public static class TypedBinder<T>
			implements BasicBinder.TypedBasicBinder<T> {

		/**
		 * The binder instance who's {@link RichBasicBinder#bind(Instance)} method had been called
		 * to get to this {@link TypedBasicBinder}.
		 */
		private final Binder binder;
		private final Resource<T> resource;

		TypedBinder( Binder binder, Instance<T> instance ) {
			this( binder, new Resource<T>( instance, binder.availability() ) );
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
			binder.bindInternal( resource, supplier );
		}

		public void to( Constructor<? extends T> constructor ) {
			to( Suppliers.costructor( constructor ) );
		}

		public void to( T instance ) {
			andTo( instance );
		}

		/**
		 * This is to do multi-binds in the same module. The {@link Binder#multibind(Class)} methods
		 * are use when a module just does one bind but that is meant to co-exist with others from
		 * other modules.
		 */
		public void to( T instance1, T instance2 ) {
			multi().andTo( instance1 ).andTo( instance2 );
		}

		public <I extends Supplier<? extends T>> void toSupplier( Class<I> implementation ) {
			to( Suppliers.link( implementation ) );
			bindImplicitToConstructor( implementation );
		}

		public void to( Provider<? extends T> provider ) {
			to( asSupplier( provider ) );
			binder.implicit().bind( defaultInstanceOf( instanceType( Provider.class, provider ) ) ).to(
					Suppliers.instance( provider ) );
		}

		public <I extends T> void to( Class<I> implementation ) {
			if ( resource.getType().getRawType() != implementation ) {
				to( Suppliers.type( Type.raw( implementation ) ) );
			}
			bindImplicitToConstructor( implementation );
		}

		protected final TypedBinder<T> multi() {
			return new TypedBinder<T>( binder.multi(), resource );
		}

		private TypedBinder<T> andTo( T instance ) {
			to( Suppliers.instance( instance ) );
			return this;
		}

		protected final void bindImplicitToConstructor( Class<?>... implementations ) {
			for ( Class<?> i : implementations ) {
				bindImplicitToConstructor( i );
			}
		}

		protected final <I> void bindImplicitToConstructor( Class<I> implementation ) {
			Constructor<I> constructor = binder.strategy().defaultConstructor( implementation );
			if ( constructor != null ) {
				binder.implicit().bind( implementation ).to( constructor );
			}
		}

	}
}
