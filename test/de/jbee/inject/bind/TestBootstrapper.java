package de.jbee.inject.bind;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.DependencyResolver;

public class TestBootstrapper {

	private static class CycleOneBundle
			extends DirectBundle {

		@Override
		protected void bootstrap() {
			install( CycleTwoBundle.class );
		}

	}

	private static class CycleTwoBundle
			extends DirectBundle {

		@Override
		protected void bootstrap() {
			install( CycleOneBundle.class );
		}

	}

	/**
	 * The assert itself doesn't play such huge role here. we just want to reach this code.
	 */
	@Test
	public void thatBundlesAreNotBootstrappedMultipleTimesEvenIfTheyHaveCycles() {
		DependencyResolver injector = Bootstrap.injector( CycleOneBundle.class );
		assertThat( injector, notNullValue() );
	}

}
