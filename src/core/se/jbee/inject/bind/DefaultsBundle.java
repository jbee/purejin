package se.jbee.inject.bind;

import se.jbee.inject.declare.Bundle;

/**
 * A {@link Bundle} that installs all features active by default that are build
 * in user space.
 * 
 * @since 19.1
 */
public final class DefaultsBundle extends BootstrapperBundle {

	@Override
	protected void bootstrap() {
		install(Adapter.SUB_CONTEXT, Adapter.ENV);
		install(DefaultScopes.class);
		install(SPIModule.class);
		install(AnnotatedWithModule.class);
	}

}
