package test.example2.car;

import se.jbee.inject.binder.Installs;
import se.jbee.inject.binder.LocalEnvModule;
import se.jbee.inject.config.NamesBy;
import se.jbee.inject.config.ProducesBy;
import se.jbee.lang.Type;
import test.example2.Car;
import test.example2.car.pool.CarPoolEnvModule;

@Installs(bundles = CarPoolEnvModule.class)
public class CarEnvModule extends LocalEnvModule {

	@Override
	protected void declare() {
		bind(NamesBy.class).to(NamesBy.DECLARED_NAME);
		bind(ProducesBy.class).to(ProducesBy.declaredMethods(false)
			.returnTypeAssignableTo(Type.raw(Car.class)));
	}
}
