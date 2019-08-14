package com.example.app;

import se.jbee.inject.bootstrap.ApplicationContextConfig;
import se.jbee.inject.config.Globals;
import se.jbee.inject.config.Options;

public class MyApplicationContextConfig implements ApplicationContextConfig {

	@Override
	public Globals globals() {
		return Globals.STANDARD.with(Options.NONE.set(Integer.class, 13));
	}
}
