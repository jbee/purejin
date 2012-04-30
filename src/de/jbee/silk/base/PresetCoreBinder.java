package de.jbee.silk.base;

import static de.jbee.silk.DefiniteType.instanceType;
import static de.jbee.silk.Instance.defaultInstance;
import de.jbee.silk.Binder;
import de.jbee.silk.DefiniteType;
import de.jbee.silk.Instance;
import de.jbee.silk.Provider;
import de.jbee.silk.Supplier;
import de.jbee.silk.Suppliers;

public class PresetCoreBinder
		implements PresetBinder {

	private final Binder binder;

	PresetCoreBinder( Binder binder ) {
		super();
		this.binder = binder;
	}

	@Override
	public <T> TypedCoreBinder<T> bind( Instance<T> instance ) {
		// TODO Auto-generated method stub
		return null;
	}

	public <T> TypedCoreBinder<T> bind( DefiniteType<T> type ) {
		return bind( Instance.defaultInstance( type ) );
	}

	public <T> TypedCoreBinder<T> bind( Class<T> type ) {
		return bind( DefiniteType.type( type ) );
	}

	static class TypedCoreBinder<T>
			implements TypedBinder<T> {

		/**
		 * The binder instance who's {@link PresetBinder#bind(Instance)} method had been called to
		 * get to this {@link TypedBinder}.
		 */
		private final PresetBinder binder;

		TypedCoreBinder( PresetBinder binder ) {
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
			to( Suppliers.type( DefiniteType.type( implementation ) ) );
		}

	}
}
