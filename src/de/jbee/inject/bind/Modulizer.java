package de.jbee.inject.bind;

import de.jbee.inject.Suppliable;

public interface Modulizer {

	Suppliable<?>[] install( Module[] modules );
}
