package test.example.app;

import se.jbee.inject.Env;
import se.jbee.inject.Extends;
import se.jbee.inject.binder.BinderModule;

@Extends(Env.class)
public class MyEnvSetupBundle extends BinderModule {

	@Override
	protected void declare() {
		bind(Integer.class).to(13);
	}

}
