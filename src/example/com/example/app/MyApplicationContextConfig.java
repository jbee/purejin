package com.example.app;

import se.jbee.inject.config.Globals;
import se.jbee.inject.config.Options;

public class MyApplicationContextConfig {

	//FIXME do something equal with new way
	public Globals globals() {
		return Globals.STANDARD.with(Options.NONE.set(Integer.class, 13));
	}
}
