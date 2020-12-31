package test.example2.car;

import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Installs;
import test.example2.car.pool.CarPoolModule;

@Installs(bundles = CarPoolModule.class)
public class CarModule extends BinderModule {

	@Override
	protected void declare() {
		autobind().in(CarService.class);
	}

}
