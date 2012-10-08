package de.jbee.inject.util;

import de.jbee.inject.Injector;
import de.jbee.inject.Injectron;

public interface InjectronSource {

	Injectron<?>[] exportTo( Injector injector );
}
