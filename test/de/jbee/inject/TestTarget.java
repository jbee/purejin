package de.jbee.inject;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Target.targeting;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class TestTarget {

	@Test
	public void thatTargetInstancesNeedsToBeMatchedByDependencies() {
		Target target = targeting( List.class );
		Dependency<String> dependency = dependency( String.class );
		assertFalse( target.isApplicableFor( dependency ) );
		assertTrue( target.isAccessibleFor( dependency.injectingInto( List.class ) ) );
	}
}
