package se.jbee.inject.bind.serviceloader;

import se.jbee.inject.Env;
import se.jbee.inject.declare.Bundle;
import se.jbee.inject.declare.Extends;

public class ServiceLoaderEnvBundles extends FilteredServiceLoaderBundles {

	public ServiceLoaderEnvBundles() {
		super(ServiceLoaderEnvBundles::isTargetingEnv);
	}

	static boolean isTargetingEnv(Class<? extends Bundle> bundle) {
		return bundle.isAnnotationPresent(Extends.class)
			&& bundle.getAnnotation(Extends.class).value() == Env.class;
	}

}
