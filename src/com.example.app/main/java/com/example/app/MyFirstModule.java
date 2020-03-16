package com.example.app;

import se.jbee.inject.bind.BinderModuleWith;

public class MyFirstModule extends BinderModuleWith<Integer> {

	@Override
	protected void declare(Integer value) {
		// the value is defined in the Env, in this case it is from
		// MyEnvSetupBundle which sets it to 13
		bind(int.class).to(value);
		bind(String.class).to(getClass().getName());
	}

}
