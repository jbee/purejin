package de.jbee.inject.bind;

import de.jbee.inject.Suppliable;

public interface Assembler { // Packager, Bundler

	Suppliable<?>[] assemble( Module[] modules );
}
