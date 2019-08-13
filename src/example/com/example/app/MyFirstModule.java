package com.example.app;

import se.jbee.inject.bind.BinderModule;

public class MyFirstModule extends BinderModule {

	@Override
	protected void declare() {
		bind(int.class).to(13);
		bind(String.class).to(getClass().getName());
	}

}
