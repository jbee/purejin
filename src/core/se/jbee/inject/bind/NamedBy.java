package se.jbee.inject.bind;

import se.jbee.inject.Name;

public final class NamedBy {

	public static final Naming<Enum<?>> ENUM = new EnumNaming<Enum<?>>();

	private NamedBy() {
		throw new UnsupportedOperationException( "util" );
	}

	private static final class EnumNaming<E extends Enum<?>>
			implements Naming<E> {

		EnumNaming() {
			//make visible
		}

		@Override
		public Name name( E value ) {
			return Name.named( value );
		}

	}
}
