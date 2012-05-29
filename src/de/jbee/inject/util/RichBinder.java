package de.jbee.inject.util;

import static de.jbee.inject.Instance.defaultInstanceOf;
import static de.jbee.inject.Suppliers.asSupplier;
import static de.jbee.inject.Type.instanceType;
import static de.jbee.inject.Type.rawType;
import de.jbee.inject.Availability;
import de.jbee.inject.Binder;
import de.jbee.inject.Instance;
import de.jbee.inject.Provider;
import de.jbee.inject.Resource;
import de.jbee.inject.Scope;
import de.jbee.inject.Scoped;
import de.jbee.inject.Source;
import de.jbee.inject.Supplier;
import de.jbee.inject.Suppliers;
import de.jbee.inject.Type;
import de.jbee.inject.util.BasicBinder.RootBinder;
import de.jbee.inject.util.BasicBinder.ScopedBinder;
import de.jbee.inject.util.BasicBinder.TargetedBinder;
import de.jbee.inject.util.BasicBinder.TypedBinder;

public class RichBinder {

	public static RichRootBinder root( Binder binder, Source source ) {
		return new RichRootBinder( binder, source );
	}

	public static class RichBasicBinder
			implements BasicBinder {

		private final Binder binder;
		private final Source source;
		private final Scope scope;
		private final Availability availability;

		RichBasicBinder( Binder binder, Source source, Scope scope, Availability availability ) {
			super();
			this.binder = binder;
			this.source = source;
			this.scope = scope;
			this.availability = availability;
		}

		@Override
		public <T> RichTypedBinder<T> bind( Instance<T> instance ) {
			return new RichTypedBinder<T>( this, instance );
		}

		public <T> RichTypedBinder<T> bind( Type<T> type ) {
			return bind( Instance.defaultInstanceOf( type ) );
		}

		public <T> RichTypedBinder<T> bind( Class<T> type ) {
			return bind( Type.rawType( type ) );
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

		final Binder binder() {
			return binder;
		}
	}

	public static class RichRootBinder
			extends RichScopedBinder
			implements RootBinder {

		RichRootBinder( Binder binder, Source source ) {
			super( binder, source, Scoped.DEFAULT );
		}

		@Override
		public RichScopedBinder in( Scope scope ) {
			return new RichScopedBinder( binder(), source(), scope );
		}
	}

	public static class RichScopedBinder
			extends RichTargetedBinder
			implements ScopedBinder {

		RichScopedBinder( Binder binder, Source source, Scope scope ) {
			super( binder, source, scope );
		}

		// means when the type/instance is created and dependencies are injected into it
		public RichTargetedBinder injectingInto( Class<?> target ) {
			return injectingInto( defaultInstanceOf( rawType( target ) ) );
		}

		@Override
		public RichTargetedBinder injectingInto( Instance<?> target ) {
			return new RichTargetedBinder( binder(), source(), scope(), target );
		}

	}

	public static class RichTargetedBinder
			extends RichBasicBinder
			implements TargetedBinder {

		RichTargetedBinder( Binder binder, Source source, Scope scope ) {
			super( binder, source, scope, Availability.EVERYWHERE );
		}

		RichTargetedBinder( Binder binder, Source source, Scope scope, Instance<?> target ) {
			super( binder, source, scope, Availability.availability( target ) );
		}

		//TODO improve this since from a dependency point of view it is good to localize all binds somehow
		// instead of narrow explicit we could expose explicit and make binds as narrow as possible by default (classic interface to impl binds in same package)

		public RichBasicBinder inPackageOf( Class<?> packageOf ) {
			return this;
		}

		public RichBasicBinder belowPackageOf( Class<?> packageOf ) {
			return this;
		}

		public RichBasicBinder beneathPackageOf( Class<?> packageOf ) {
			return this;
		}
	}

	public static class RichTypedBinder<T>
			implements BasicBinder.TypedBinder<T> {

		/**
		 * The binder instance who's {@link RichBasicBinder#bind(Instance)} method had been called
		 * to get to this {@link TypedBinder}.
		 */
		private final RichBasicBinder builder;
		private final Resource<T> resource;

		RichTypedBinder( RichBasicBinder binder, Instance<T> instance ) {
			super();
			this.builder = binder;
			this.resource = new Resource<T>( instance, binder.availability() );
		}

		@Override
		public void to( Supplier<? extends T> supplier ) {
			builder.binder().bind( resource, supplier, builder.scope(), builder.source() );
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
			to( Suppliers.type( Type.rawType( implementation ) ) );
		}

	}
}
