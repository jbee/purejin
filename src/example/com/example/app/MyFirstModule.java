package com.example.app;

import se.jbee.inject.bind.BinderModuleWith;

public class MyFirstModule extends BinderModuleWith<Integer> {

	@Override
	protected void declare(Integer value) {
		bind(int.class).to(value);
		bind(String.class).to(getClass().getName());
	}

}
