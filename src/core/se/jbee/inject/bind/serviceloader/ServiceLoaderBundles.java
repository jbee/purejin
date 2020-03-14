package se.jbee.inject.bind.serviceloader;

import se.jbee.inject.Injector;
import se.jbee.inject.declare.Bundle;
import se.jbee.inject.declare.Extends;

public class ServiceLoaderBundles extends FilteredServiceLoaderBundles {

	public ServiceLoaderBundles() {
		super(ServiceLoaderBundles::isTargetingInjector);
	}

	static boolean isTargetingInjector(Class<? extends Bundle> bundle) {
		return !bundle.isAnnotationPresent(Extends.class)
			|| bundle.getAnnotation(Extends.class).value() == Injector.class;
	}
}
