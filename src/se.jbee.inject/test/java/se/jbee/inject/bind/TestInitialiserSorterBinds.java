package se.jbee.inject.bind;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.container.Initialiser;

/**
 * A test that demonstrates how the {@link Initialiser.Sorter} can be used to
 * order {@link Initialiser}s.
 * 
 * In this example the initialisers are sorted in reverse alphabetically order
 * of their class names. One of them is a lambda which means the name starts
 * with the name of the surrounding {@link TestInitialiserSorterBinds} class.
 */
public class TestInitialiserSorterBinds {

	static final class Bean {

		List<String> names = new ArrayList<>();
	}

	static final class TestInitialiserSorterBindsModule extends BinderModule
			implements Initialiser.Sorter {

		@Override
		protected void declare() {
			bind(Bean.class).toConstructor();
			initbind(Bean.class).to(InitA.class);
			initbind(Bean.class).to(InitB.class);
			initbind(Bean.class).to((bean, context) -> {
				bean.names.add("c");
				return bean;
			});
			bind(Initialiser.Sorter.class).to(this);
		}

		@Override
		public Initialiser<?>[] sort(Class<?> actualType,
				Initialiser<?>[] set) {
			Arrays.sort(set, (a, b) -> b.getClass().getSimpleName().compareTo(
					a.getClass().getSimpleName()));
			return set;
		}

	}

	static final class InitA implements Initialiser<Bean> {

		@Override
		public Bean init(Bean bean, Injector context) {
			bean.names.add("a");
			return bean;
		}

	}

	static final class InitB implements Initialiser<Bean> {

		@Override
		public Bean init(Bean bean, Injector context) {
			bean.names.add("b");
			return bean;
		}

	}

	private final Injector injector = Bootstrap.injector(
			TestInitialiserSorterBindsModule.class);

	@Test
	public void orderOfInitialisersCanBeCustomisedByDefiningSorter() {
		assertEquals(asList("c", "b", "a"), injector.resolve(Bean.class).names);
	}
}
