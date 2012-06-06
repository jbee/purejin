package de.jbee.inject.util;

import static de.jbee.inject.Instance.defaultInstanceOf;
import static de.jbee.inject.Instance.instance;
import static de.jbee.inject.Suppliers.asSupplier;
import static de.jbee.inject.Type.instanceType;
import static de.jbee.inject.Type.raw;
import de.jbee.inject.Availability;
import de.jbee.inject.BindDeclarator;
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

	public static RootBinder create( BindDeclarator declarator, Source source ) {
		return new RootBinder( declarator, source );
	}

	private final BindDeclarator declarator;
	private final Source source;
	private final Scope scope;
	private final Availability availability;

	Binder( BindDeclarator declarator, Source source, Scope scope, Availability availability ) {
		super();
		this.declarator = declarator;
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

	public <E> TypedMultiBinder<E> bind( Class<E[]> type ) {

		return null;
	}

	public <T> TypedBinder<T> autobind( Type<T> type ) {
		//FIXME need more bindings when chain is done - how to do that ?
		return bind( type );
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

	final BindDeclarator declarator() {
		return declarator;
	}

	protected final Binder with( Source source ) {
		return new Binder( declarator, source, scope, availability );
	}

	protected final Binder with( Availability availability ) {
		return new Binder( declarator, source, scope, availability );
	}

	public static class TypedMultiBinder<E>
			extends TypedBinder<E[]> {

		TypedMultiBinder( Binder binder, Instance<E[]> instance ) {
			super( binder, instance );
		}

		void to( Class<? extends E> src1, Class<? extends E> src2 ) {
		}

		void to( Class<? extends E> src1, Class<? extends E> src2, Class<? extends E> src3 ) {

		}

		// and so on.... will avoid generic array warning here 
	}

	public static class RootBinder
			extends ScopedBinder
			implements RootBasicBinder {

		RootBinder( BindDeclarator declarator, Source source ) {
			super( declarator, source, Scoped.DEFAULT );
		}

		@Override
		public ScopedBinder in( Scope scope ) {
			return new ScopedBinder( declarator(), source(), scope );
		}
	}

	public static class ScopedBinder
			extends TargetedBinder
			implements ScopedBasicBinder {

		ScopedBinder( BindDeclarator declarator, Source source, Scope scope ) {
			super( declarator, source, scope );
		}

		// means when the type/instance is created and dependencies are injected into it
		public TargetedBinder injectingInto( Class<?> target ) {
			return injectingInto( defaultInstanceOf( raw( target ) ) );
		}

		@Override
		public TargetedBinder injectingInto( Instance<?> target ) {
			return new TargetedBinder( declarator(), source(), scope(), target );
		}

	}

	public static class TargetedBinder
			extends Binder
			implements TargetedBasicBinder {

		TargetedBinder( BindDeclarator declarator, Source source, Scope scope ) {
			super( declarator, source, scope, Availability.EVERYWHERE );
		}

		TargetedBinder( BindDeclarator declarator, Source source, Scope scope, Instance<?> target ) {
			super( declarator, source, scope, Availability.availability( target ) );
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
		private final Binder builder;
		private final Resource<T> resource;

		TypedBinder( Binder binder, Instance<T> instance ) {
			super();
			this.builder = binder;
			this.resource = new Resource<T>( instance, binder.availability() );
		}

		@Override
		public void to( Supplier<? extends T> supplier ) {
			builder.declarator().bind( resource, supplier, builder.scope(), builder.source() );
		}

		public void to( T instance ) {
			to( Suppliers.instance( instance ) );
		}

		public void toSupplier( Class<? extends Supplier<? extends T>> supplier ) {
			try {
				to( supplier.newInstance() );
			} catch ( InstantiationException e ) {
				throw new RuntimeException( e );
			} catch ( IllegalAccessException e ) {
				throw new RuntimeException( e );
			}
		}

		public void to( Provider<? extends T> provider ) {
			to( asSupplier( provider ) );
			builder.bind( defaultInstanceOf( instanceType( Provider.class, provider ) ) ).to(
					Suppliers.instance( provider ) ); //TODO implicit source ?
		}

		public void to( Class<? extends T> implementation ) {
			to( Suppliers.type( Type.raw( implementation ) ) );
			//FIXME find best-match constructor and bind impl-class implicit to that constructor 
			//(constructor resolution could also be made in a special supplier but I guess its better to make that a binders task) 
		}

	}
}
