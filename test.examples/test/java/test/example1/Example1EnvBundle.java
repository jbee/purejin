package test.example1;

import se.jbee.inject.binder.EnvModule;

public class Example1EnvBundle extends EnvModule {

	@Override
	protected void declare() {
		bind(Integer.class).to(13);
	}

}
