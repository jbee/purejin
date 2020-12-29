package test.example1;

import se.jbee.inject.binder.BootstrapperBundle;

public class MyRootBundle extends BootstrapperBundle {

	@Override
	protected void bootstrap() {
		install(MyFirstModule.class);
	}

}
