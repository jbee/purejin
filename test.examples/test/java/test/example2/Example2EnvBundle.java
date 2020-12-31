package test.example2;

import se.jbee.inject.Env;
import se.jbee.inject.Extends;
import se.jbee.inject.binder.BootstrapperBundle;
import test.example2.bus.BusEnvModule;
import test.example2.car.CarEnvModule;

@Extends(Env.class)
public class Example2EnvBundle extends BootstrapperBundle {

	@Override
	protected void bootstrap() {
		install(CarEnvModule.class);
		install(BusEnvModule.class);
	}
}
