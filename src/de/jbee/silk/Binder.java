package de.jbee.silk;

import static de.jbee.silk.ClassType.classtype;
import static de.jbee.silk.Instance.defaultInstance;

public class Binder
		implements PresetBinder {

	@Override
	public <T> TypedCoreBinder<T> bind( Instance<T> instance ) {
		// TODO Auto-generated method stub
		return null;
	}

	public <T> TypedCoreBinder<T> bind( ClassType<T> type ) {
		return bind( Instance.defaultInstance( type ) );
	}

	public <T> TypedCoreBinder<T> bind( Class<T> type ) {
		return bind( ClassType.type( type ) );
	}

	static class TypedCoreBinder<T>
			implements TypedBinder<T> {

		/**
		 * The binder instance who's {@link PresetBinder#bind(Instance)} method had been called to get to
		 * this {@link TypedBinder}.
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
			to( Suppliers.valueFromProvider( provider ) );
			binder.bind( defaultInstance( classtype( Provider.class, provider ) ) ).to(
					Suppliers.instance( provider ) );
		}

		public void to( Class<? extends T> implementation ) {
			to( Suppliers.type( ClassType.type( implementation ) ) );
		}

	}
}
