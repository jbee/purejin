package de.jbee.inject.bind;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.DependencyResolver;

public class TestBootstrapper {

	private static class OneBundle
			extends DirectBundle {

		@Override
		protected void bootstrap() {
			install( OtherBundle.class );
		}

	}

	private static class OtherBundle
			extends DirectBundle {

		@Override
		protected void bootstrap() {
			install( OneBundle.class );
		}

	}

	@Test
	public void thatBundleCyclesDontCauseStackOverflowErrors() {
		DependencyResolver injector = Bootstrap.injector( OneBundle.class );
		assertThat( injector, notNullValue() );
	}
}
