package de.jbee.inject.draft;

import java.lang.reflect.Constructor;

import de.jbee.inject.Instance;

public interface ParameterStrategy {

	/**
	 * OPEN this is in some way against the concept of silk because the information which instances
	 * belong where came from the code so this should be something one could build on top of silk
	 * but not a core part ---> e.g. HintStrategy interface build in builder - user could create an
	 * constant that looks for hints
	 */
	<T> Instance<?>[] parametersFor( Constructor<T> constructor );
}
