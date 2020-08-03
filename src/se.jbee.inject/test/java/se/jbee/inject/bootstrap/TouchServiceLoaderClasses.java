package se.jbee.inject.bootstrap;

import org.junit.Test;
import se.jbee.inject.binder.ServiceLoaderAnnotations;
import se.jbee.inject.binder.ServiceLoaderBundles;
import se.jbee.inject.binder.ServiceLoaderEnvBundles;

public class TouchServiceLoaderClasses {

	@SuppressWarnings("unused")
	@Test
	public void test() {
		new ServiceLoaderAnnotations();
		new ServiceLoaderBundles();
		new ServiceLoaderEnvBundles();
	}

}
