package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Target.targeting;

import java.util.List;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Target;

public class TestTarget {

	@Test
	public void thatTargetInstancesNeedsToBeMatchedByDependencies() {
		Target target = targeting(List.class);
		Dependency<String> dependency = dependency(String.class);
		assertFalse(target.isAvailableFor(dependency));
		assertTrue(
				target.isAccessibleFor(dependency.injectingInto(List.class)));
	}
}
