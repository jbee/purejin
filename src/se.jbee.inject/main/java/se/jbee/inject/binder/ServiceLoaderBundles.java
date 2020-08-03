package se.jbee.inject.binder;

import se.jbee.inject.Extends;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.Bundle;

public class ServiceLoaderBundles extends FilteredServiceLoaderBundles {

	public ServiceLoaderBundles() {
		super(ServiceLoaderBundles::isTargetingInjector);
	}

	static boolean isTargetingInjector(Class<? extends Bundle> bundle) {
		return !bundle.isAnnotationPresent(Extends.class)
			|| bundle.getAnnotation(Extends.class).value() == Injector.class;
	}
}
