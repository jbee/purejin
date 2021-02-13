package test.example2.bus;

import se.jbee.inject.binder.LocalEnvModule;
import se.jbee.inject.config.NamesBy;
import se.jbee.inject.config.ProducesBy;
import test.example2.Bus;

import static se.jbee.lang.Type.raw;

public class BusEnvModule extends LocalEnvModule {

	@Override
	protected void declare() {
		bind(NamesBy.class).to(NamesBy.DECLARED_NAME);
		bind(ProducesBy.class).to(ProducesBy.declaredMethods(false)
				.returnTypeAssignableTo(raw(Bus.class)));
	}
}
