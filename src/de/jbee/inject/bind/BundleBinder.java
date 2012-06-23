package de.jbee.inject.bind;

import de.jbee.inject.Suppliable;

public interface BundleBinder {

	Suppliable<?>[] install( Class<? extends Bundle> root );
}
