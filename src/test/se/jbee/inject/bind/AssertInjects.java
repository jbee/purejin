package se.jbee.inject.bind;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Dependency.dependency;

import java.util.Collection;

import se.jbee.inject.Injector;
import se.jbee.inject.Type;

public class AssertInjects {

	private final Injector injector;

	public AssertInjects( Injector injector ) {
		super();
		this.injector = injector;
	}

	public <T> void assertInjects( T expected, Type<? extends T> dependencyType ) {
		assertThat( injector.resolve( dependency( dependencyType ) ), is( expected ) );
	}

	public <E> void assertInjectsItems( E[] expected, Type<? extends Collection<?>> dependencyType ) {
		assertInjectsItems( asList( expected ), dependencyType );
	}

	public <E> void assertInjectsItems( Collection<E> expected,
			Type<? extends Collection<?>> dependencyType ) {
		assertTrue( injector.resolve( dependency( dependencyType ) ).containsAll( expected ) );
	}
}
