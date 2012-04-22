package de.jbee.silk;

public class CoreBinder
		implements Binder {

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
			to( Suppliers.instance( instance ) );
		}

		public void toSupplier( Class<? extends Supplier<? extends T>> supplier ) {
			// TODO somehow ensure there is a single instance of that supplier class given (created and used)

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
