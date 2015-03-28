package se.jbee.inject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Target.targeting;

import java.util.List;

import org.junit.Test;

public class TestTarget {

	@Test
	public void thatTargetInstancesNeedsToBeMatchedByDependencies() {
		Target target = targeting( List.class );
		Dependency<String> dependency = dependency( String.class );
		assertFalse( target.isAvailableFor( dependency ) );
		assertTrue( target.isAccessibleFor( dependency.injectingInto( List.class ) ) );
	}
}
