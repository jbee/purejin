package de.jbee.inject.bind;

import static de.jbee.inject.Name.named;
import static de.jbee.inject.Type.raw;
import de.jbee.inject.Dependency;
import de.jbee.inject.Name;

public final class Extend {

	public static <E extends Enum<E> & Extension<E, ? super T>, T> Name name( Class<E> extension,
			Class<? extends T> type ) {
		return named( extension.getCanonicalName() + ":" + type.getCanonicalName() );
	}

	public static <E extends Enum<E> & Extension<E, ? super T>, T> Name name( E extension,
			Class<? extends T> type ) {
		return named( extension.getClass().getCanonicalName() + ":" + extension.name() + ":"
				+ type.getCanonicalName() );
	}

	public static <E extends Enum<E> & Extension<E, ? super T>, T> Dependency<Class[]> dependency(
			Class<E> extension ) {
		return Dependency.dependency( raw( Class[].class ).parametizedAsLowerBounds() ).named(
				Name.prefixed( extension.getCanonicalName() + ":" ) );
	}
}
