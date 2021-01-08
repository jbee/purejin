package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Hint;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModuleWith;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.Connector;
import se.jbee.inject.config.ProducesBy;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Name.named;

/**
 * This test demonstrates the basic feature of dynamically connection methods
 * for different types of processing.
 */
class TestFeatureConnectorBinds {

	/**
	 * Annotations are just one way to select methods used by
	 * the test examples in this class
	 */
	@Target(METHOD)
	@Retention(RUNTIME)
	@interface Marked {}

	/**
	 * Connecting by default applies to type hierarchies so interfaces can be
	 * useful to describe the set of {@link Class}es where member {@link
	 * Method}s potentially should be found.
	 * <p>
	 * In this example the interface is also used to describe the set of {@link
	 * Method}s that are considered members.
	 */
	@FunctionalInterface
	interface Cleaned {
		void clean();
	}

	public static class Bean implements Cleaned {

		final String name;
		boolean cleaned;

		public Bean(String name) {
			this.name = name;
		}

		@Marked
		public void myMethod() {

		}

		@Override
		public void clean() {
			cleaned = true;
		}

		@Override
		public String toString() {
			return name; // just for debug and to have another unmarked method
		}
	}

	private static class TestFeatureConnectorBindsModule
			extends BinderModuleWith<Connector> {

		@Override
		protected void declare(Connector verifier) {
			// the marking as "my-linker"
			injectingInto("marked", Bean.class) //
					.connect(ProducesBy.OPTIMISTIC.annotatedWith(Marked.class)) //
					.inAny(Bean.class) //
					.to("my-linker");

			// 2 named instances, one should match for marking, other should not
			bind(named("marked"), Bean.class).toConstructor(Hint.constant("marked"));
			bind(named("unmarked"), Bean.class).toConstructor(Hint.constant("unmarked"));

			// whom to call with marked methods of: my-linker
			bind("my-linker", Connector.class).to(verifier);

			//-----------------[2nd group]-----------------------
			connect(Cleaned.class).to("my-cleaned");

			bind("my-cleaned", Connector.class).to((instance, as, method) -> {
				// in this example: directly call the method to verify it got passed
				try {
					method.invoke(instance);
				} catch (Exception e) {
					fail(e);
				}
			});
		}
	}

	@Test
	void injectingIntoCanBeUsedToLimitTheSetOfAffectedInstances() {
		List<Object> acceptedInstances = new ArrayList<>();
		List<Method> acceptedMethods = new ArrayList<>();
		Connector verifier = (instance, as, method) -> {
			acceptedInstances.add(instance);
			acceptedMethods.add(method);
		};
		Env env = Bootstrap.DEFAULT_ENV.with(Connector.class, verifier);
		Injector context = Bootstrap.injector(env, TestFeatureConnectorBindsModule.class);
		Bean expected = context.resolve("marked", Bean.class);
		assertEquals("marked", expected.name);
		assertEquals("unmarked", context.resolve("unmarked", Bean.class).name);
		assertEquals(singletonList(expected), acceptedInstances);
		assertEquals(1, acceptedMethods.size());
		assertEquals("myMethod", acceptedMethods.get(0).getName());
	}

	@Test
	void interfacesApplyDynamicLinkingToAssignableTypes() {
		Env env = Bootstrap.DEFAULT_ENV.with(Connector.class, (i, as, m) -> {});
		Injector context = Bootstrap.injector(env, TestFeatureConnectorBindsModule.class);
		assertTrue(context.resolve("marked", Bean.class).cleaned,
				"clean was not called for marked bean");
		assertTrue(context.resolve("unmarked", Bean.class).cleaned,
				"clean was not called for unmarked bean");
	}
}
