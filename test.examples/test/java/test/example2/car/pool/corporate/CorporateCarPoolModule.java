package test.example2.car.pool.corporate;

import se.jbee.inject.binder.BinderModule;

public class CorporateCarPoolModule extends BinderModule {

	@Override
	protected void declare() {
		autobind().in(CorporateCarPoolService.class);
	}
}
