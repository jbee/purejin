package test.example1;

import se.jbee.inject.binder.BootstrapperBundle;

public class Example1RootBundle extends BootstrapperBundle {

	@Override
	protected void bootstrap() {
		install(Example1Module.class);
	}

}
