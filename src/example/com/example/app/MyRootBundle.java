package com.example.app;

import se.jbee.inject.bootstrap.BootstrapperBundle;

public class MyRootBundle extends BootstrapperBundle {

	@Override
	protected void bootstrap() {
		install(MyFirstModule.class);
	}

}