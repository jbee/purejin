package com.example.app;

import se.jbee.inject.Env;
import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.declare.Extends;

@Extends(Env.class)
public class MyEnvSetupBundle extends BinderModule {

	@Override
	protected void declare() {
		bind(Integer.class).to(13);
	}

}
