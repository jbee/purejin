package de.jbee.inject.bind;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.DependencyResolver;

/**
 * The tests shows an example of cyclic depended {@link Bundle}s. It shows that a {@link Bundle}
 * doesn't have to know or consider other bundles since it is valid to make cyclic references or
 * install the {@link Bundle}s multiple times.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public class TestBootstrapper {

	private static class CycleOneBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( CycleTwoBundle.class );
		}

	}

	private static class CycleTwoBundle
			extends BootstrapperBundle {

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
