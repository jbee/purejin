package se.jbee.inject;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Type.raw;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class TestResource {

	@Test
	public void naturalOrderIsFromMostQualifiedToLeastQualified() {
		List<Resource<?>> resources = new ArrayList<>();
		resources.add(createResourceOf(1, Type.WILDCARD));
		resources.add(
				createResourceOf(2, raw(Serializable.class).asUpperBound()));
		resources.add(createResourceOf(3, raw(Number.class).asUpperBound()));
		Collections.sort(resources);
		assertEquals(Arrays.asList(3, 2, 1),
				resources.stream().map(e -> e.serialID).collect(toList()));
	}

	private Resource<?> createResourceOf(int serialID, Type<?> type) {
		Source source = Source.source(getClass());
		return new Resource<>(serialID, source, ScopePermanence.ignore,
				new Locator<>(Instance.anyOf(type)), resource -> (dep -> null),
				Annotated.WITH_NO_ANNOTATIONS);
	}
}
