package test.example2;

import se.jbee.inject.binder.BootstrapperBundle;
import test.example2.bus.BusModule;
import test.example2.car.CarModule;

public class Example2Bundle extends BootstrapperBundle  {

	@Override
	protected void bootstrap() {
		install(CarModule.class);
		install(BusModule.class);
	}
}
