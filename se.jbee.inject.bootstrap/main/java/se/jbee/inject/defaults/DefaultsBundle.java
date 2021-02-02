package se.jbee.inject.defaults;

import se.jbee.inject.Injector;
import se.jbee.inject.Resource;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.Module;
import se.jbee.inject.binder.BootstrapperBundle;

/**
 * A {@link Bundle} that installs all {@link DefaultFeature} features active by
 * default. This means a {@link Injector} feature is implemented by providing a
 * {@link Resource} which is bound as a usual {@link Module}.
 *
 * @since 8.1
 */
public final class DefaultsBundle extends BootstrapperBundle {

	@Override
	protected void bootstrap() {
		install(DefaultFeature.INSTALLED_BY_DEFAULT);
	}
}
