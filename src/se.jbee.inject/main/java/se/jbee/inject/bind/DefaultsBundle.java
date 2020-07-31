package se.jbee.inject.bind;

import se.jbee.inject.Injector;
import se.jbee.inject.Resource;
import se.jbee.inject.declare.Bundle;
import se.jbee.inject.declare.Module;

/**
 * A {@link Bundle} that installs all {@link CoreFeature} features active by
 * default. This means a {@link Injector} feature is implemented by providing a
 * {@link Resource} which is bound as a usual {@link Module}.
 * 
 * @since 19.1
 */
public final class DefaultsBundle extends BootstrapperBundle {

	@Override
	protected void bootstrap() {
		for (CoreFeature f : CoreFeature.values())
			if (f.installedByDefault)
				install(f);
	}

}
