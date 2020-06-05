package se.jbee.inject.bind.serviceloader;

import org.junit.Test;

public class TouchServiceLoaderClasses {

	@Test
	public void test() {
		new ServiceLoaderAnnotations();
		new ServiceLoaderBundles();
		new ServiceLoaderEnvBundles();
	}

}
