package test.example1;

import se.jbee.inject.Env;
import se.jbee.inject.Extends;
import se.jbee.inject.binder.EnvModule;

@Extends(Env.class)
public class MyEnvSetupBundle extends EnvModule {

	@Override
	protected void declare() {
		bind(Integer.class).to(13);
	}

}
