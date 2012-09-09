package de.jbee.inject.bind;

import de.jbee.inject.Suppliable;

public interface Assembler {

	Suppliable<?>[] assemble( Module[] modules, ConstructionStrategy strategy );
}
