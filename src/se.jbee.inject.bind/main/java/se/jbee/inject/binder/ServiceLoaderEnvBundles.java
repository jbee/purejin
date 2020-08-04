package se.jbee.inject.binder;

import se.jbee.inject.Env;
import se.jbee.inject.Extends;
import se.jbee.inject.bind.Bundle;

public class ServiceLoaderEnvBundles extends FilteredServiceLoaderBundles {

	public ServiceLoaderEnvBundles() {
		super(ServiceLoaderEnvBundles::isTargetingEnv);
	}

	static boolean isTargetingEnv(Class<? extends Bundle> bundle) {
		return bundle.isAnnotationPresent(Extends.class)
			&& bundle.getAnnotation(Extends.class).value() == Env.class;
	}

}
