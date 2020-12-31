package test;

import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;

public enum Example {

	EXAMPLE_1,
	EXAMPLE_2;

	public Env env() {
		return Bootstrap.env(
				Bootstrap.DEFAULT_ENV.withDependent(Example.class, this));
	}

	public Injector injector() {
		return Bootstrap.injector(env().withDependent(Example.class, this));
	}
}
