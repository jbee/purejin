package test.example2.bus;

import se.jbee.inject.binder.BinderModule;

public class BusModule extends BinderModule {
	@Override
	protected void declare() {
		autobind().in(BusService.class);
	}
}
