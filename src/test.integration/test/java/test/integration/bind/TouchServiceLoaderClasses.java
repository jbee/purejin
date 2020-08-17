package test.integration.bind;

import org.junit.jupiter.api.Test;
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
