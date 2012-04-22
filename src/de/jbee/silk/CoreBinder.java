package de.jbee.silk;

public class CoreBinder
		implements Binder {

	@Override
	public <T> TypedCoreBinder<T> bind( Instance<T> instance ) {
		// TODO Auto-generated method stub
		return null;
	}

	public <T> TypedCoreBinder<T> bind( ClassType<T> type ) {

		return null;
	}

	public <T> TypedCoreBinder<T> bind( Class<T> type ) {

		return null;
	}

	static class TypedCoreBinder<T>
			implements TypedBinder<T> {

		/**
		 * The binder instance who's {@link Binder#bind(Instance)} method had been called to get to
		 * this {@link TypedBinder}.
		 */
		private final CoreBinder binder;

		TypedCoreBinder( CoreBinder binder ) {
			super();
			this.binder = binder;
		}

		@Override
		public void to( Supplier<? extends T> supplier ) {
			// TODO Auto-generated method stub

		}

		public void to( T instance ) {

		}

		public void toSupplier( Class<? extends Supplier<? extends T>> supplier ) {
			// TODO Auto-generated method stub

		}

		public void to( Provider<? extends T> provider ) {
			to( Suppliers.valueFromProvider( provider ) );
			binder.bind( ClassType.type( Provider.class, provider ) ).to( provider );
		}

		public void to( Class<? extends T> implementation ) {
			to( Suppliers.type( ClassType.type( implementation ) ) );
		}

	}
}
