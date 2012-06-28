package de.jbee.inject;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Target.targeting;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class TestTarget {

	@Test
	public void test() {
		Target t = targeting( List.class );
		Dependency<String> d = dependency( String.class );
		assertFalse( t.isApplicableFor( d ) );
		assertTrue( t.isAccessibleFor( d.injectingInto( List.class ) ) );
	}
}
