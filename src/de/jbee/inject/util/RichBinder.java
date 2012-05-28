package de.jbee.inject.util;

import static de.jbee.inject.Instance.defaultInstance;
import static de.jbee.inject.Type.instanceType;
import de.jbee.inject.Binder;
import de.jbee.inject.Instance;
import de.jbee.inject.Provider;
import de.jbee.inject.Supplier;
import de.jbee.inject.Suppliers;
import de.jbee.inject.Type;
import de.jbee.inject.util.BasicBinder.TypedBinder;
import de.jbee.inject.util.RichBinder.RichBasicBinder.RichRootBinder;

public class RichBinder {

	public static RichRootBinder root( Binder binder ) {
		return null;
	}

	public static class RichBasicBinder
			implements BasicBinder {

		private final Binder binder;

		RichBasicBinder( Binder binder ) {
			super();
			this.binder = binder;
		}

		@Override
		public <T> RichTypedBinder<T> bind( Instance<T> instance ) {
			// TODO Auto-generated method stub
			return null;
		}

		public <T> RichTypedBinder<T> bind( Type<T> type ) {
			return bind( Instance.defaultInstance( type ) );
		}

		public <T> RichTypedBinder<T> bind( Class<T> type ) {
			return bind( Type.rawType( type ) );
		}

		public static abstract class RichRootBinder
				implements RootBinder {

			public RichRootBinder( Binder binder ) {

			}
		}
	}

	public static class RichTypedBinder<T>
			implements BasicBinder.TypedBinder<T> {

		/**
		 * The binder instance who's {@link RichBasicBinder#bind(Instance)} method had been called
		 * to get to this {@link TypedBinder}.
		 */
		private final RichBasicBinder binder;

		RichTypedBinder( RichBasicBinder binder ) {
			super();
			this.binder = binder;
		}

		@Override
		public void to( Supplier<? extends T> supplier ) {
			// TODO Auto-generated method stub

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
			to( Suppliers.adapt( provider ) );
			binder.bind( defaultInstance( instanceType( Provider.class, provider ) ) ).to(
					Suppliers.instance( provider ) );
		}

		public void to( Class<? extends T> implementation ) {
			to( Suppliers.type( Type.rawType( implementation ) ) );
		}

	}
}
