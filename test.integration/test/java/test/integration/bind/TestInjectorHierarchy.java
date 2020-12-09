package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.lang.Type;
import test.integration.container.Decorate;

import java.util.HashSet;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test that demonstrates how two {@link Injector} contexts can be linked
 * together in a hierarchy using the {@link Decorate} utility.
 */
class TestInjectorHierarchy {

	/**
	 * The shared context
	 */
	static final class TestInjectorHierarchyRootContext extends BinderModule {

		@Override
		protected void declare() {
			bind(Integer.class).to(42);
			bind(Float.class).to(13f);
		}

	}

	/**
	 * The context only visible in branch 1
	 */
	static final class TestInjectorHierarchyBranch1Context
			extends BinderModule {

		@Override
		protected void declare() {
			bind(Float.class).to(42.42f);
		}

	}

	/**
	 * The context only visible in branch 2
	 */
	static final class TestInjectorHierarchyBranch2Context
			extends BinderModule {

		@Override
		protected void declare() {
			bind(Float.class).to(0.42f);
		}

	}

	private final Injector root = Bootstrap.injector(
			TestInjectorHierarchyRootContext.class);
	private final Injector branch1 = Bootstrap.injector(
			TestInjectorHierarchyBranch1Context.class);
	private final Injector branch2 = Bootstrap.injector(
			TestInjectorHierarchyBranch2Context.class);
	private final Injector branched1 = Decorate.hierarchy(root, branch1);
	private final Injector branched2 = Decorate.hierarchy(root, branch2);

	@Test
	public void parentContextIsAccessibleForHierarchicalInjector() {
		assertEquals(42, branched1.resolve(Integer.class).intValue());
		assertEquals(42, branched2.resolve(Integer.class).intValue());
	}

	@Test
	public void childContextIsAccessibleForHierarchicalInjector() {
		assertEquals(42.42f, branched1.resolve(Float.class).floatValue(),
				0.01f);
		assertEquals(0.42f, branched2.resolve(Float.class).floatValue(), 0.01f);
	}

	@Test
	public void mergedArrayContextIsAccessibleForHierarchicalInjector() {
		Number[] numbers = branched1.resolve(
				Type.raw(Number[].class).asUpperBound());
		assertEqualSets(new Number[] { 42, 13f, 42.42f }, numbers);
		numbers = branched2.resolve(Type.raw(Number[].class).asUpperBound());
		assertEqualSets(new Number[] { 42, 13f, 0.42f }, numbers);
	}

	private static <T> void assertEqualSets(T[] expected, T[] actual) {
		assertEquals(new HashSet<>(asList(expected)),
				new HashSet<>(asList(actual)));
	}
}
