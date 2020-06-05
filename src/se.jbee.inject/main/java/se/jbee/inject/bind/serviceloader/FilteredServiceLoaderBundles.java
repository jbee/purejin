package se.jbee.inject.bind.serviceloader;

import java.util.ServiceLoader;
import java.util.function.Predicate;

import se.jbee.inject.bind.BootstrapperBundle;
import se.jbee.inject.declare.Bundle;

public abstract class FilteredServiceLoaderBundles extends BootstrapperBundle {

	private final Predicate<Class<? extends Bundle>> filter;

	protected FilteredServiceLoaderBundles(
			Predicate<Class<? extends Bundle>> filter) {
		this.filter = filter;
	}

	@Override
	protected final void bootstrap() {
		//TODO localise effect to package
		for (Bundle bundle : ServiceLoader.load(Bundle.class)) {
			if (filter.test(bundle.getClass())) {
				install(bundle.getClass());
			}
		}
	}
}
