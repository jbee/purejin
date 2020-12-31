package test.example2.car.pool;

import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Installs;
import test.example2.car.pool.corporate.CorporateCarPoolModule;

@Installs(bundles = CorporateCarPoolModule.class)
public class CarPoolModule extends BinderModule {

	@Override
	protected void declare() {
		autobind().in(CarPoolService.class);
	}
}
