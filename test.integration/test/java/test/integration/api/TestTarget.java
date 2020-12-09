package test.integration.api;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Target;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Target.targeting;

class TestTarget {

	@Test
	public void thatTargetInstancesNeedsToBeMatchedByDependencies() {
		Target target = targeting(List.class);
		Dependency<String> dependency = dependency(String.class);
		assertFalse(target.isAvailableFor(dependency));
		assertTrue(
				target.isAccessibleFor(dependency.injectingInto(List.class)));
	}
}
