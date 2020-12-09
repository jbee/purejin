package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Hint;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModuleWith;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Environment;
import se.jbee.inject.config.DynamicLinker;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.config.ProducesBy.allMethods;

/**
 * This test demonstrates the basic feature of dynamically linking methods for later
 * processing.
 * <p>
 * While the {@link Binder#link()} identifies {@link Method}s (per {@link Object} instance
 * created in a {@link Injector} context) that are "members" of a {@link
 * se.jbee.inject.Name#named(Object)} group a {@link BiConsumer} processor of
 * matching group {@link se.jbee.inject.Name} must be bound which handles the
 * processing of member {@link Method}s for an {@link Object} instance.
 */
class TestDynamicallyLinkedMethodsBinds {

	/**
	 * Annotations are just one way to mark methods which is chosen in one of
	 * the test examples in this class
	 */
	@Target(METHOD)
	@Retention(RUNTIME)
	@interface Marked {}

	/**
	 * Marking by default applies to type hierarchies so interfaces can be
	 * useful to describe the set of {@link Class}es where member {@link
	 * Method}s potentially should be found.
	 * <p>
	 * In this example the interface is also used to describe the set of {@link
	 * Method}s that are considered members.
	 */
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

	static class TestMarkBindsModule extends BinderModuleWith<DynamicLinker> {

		@Override
		protected void declare(DynamicLinker verifier) {
			// the marking as "my-linker"
			injectingInto(named("marked"), Bean.class) //
					.link(allMethods.annotatedWith(Marked.class)) //
					.in(Bean.class) //
					.with("my-linker");

			// 2 named instances, one should match for marking, other should not
			bind(named("marked"), Bean.class).toConstructor(Hint.constant("marked"));
			bind(named("unmarked"), Bean.class).toConstructor(Hint.constant("unmarked"));

			// whom to call with marked methods of: my-linker
			bind("my-linker", DynamicLinker.class).to(verifier);

			//-----------------[2nd group]-----------------------
			link(Cleaned.class).with("my-cleaned");

			bind("my-cleaned", DynamicLinker.class).to((instance, marked) -> {
				// in this example: directly call the method to verify it got passed
				try {
					marked.invoke(instance);
				} catch (Exception e) {
					fail(e);
				}
			});
		}
	}

	@Test
	public void targetingViaInjectingIntoCanBeUsedToLimitTheSetOfInstancesAffectedByMarking() {
		List<Object> acceptedInstances = new ArrayList<>();
		List<Method> acceptedMethods = new ArrayList<>();
		DynamicLinker verifier = (instance, marked) -> {
			acceptedInstances.add(instance);
			acceptedMethods.add(marked);
		};
		Env env = Environment.DEFAULT.with(DynamicLinker.class, verifier);
		Injector context = Bootstrap.injector(env, TestMarkBindsModule.class);
		Bean expected = context.resolve("marked", Bean.class);
		assertEquals("marked", expected.name);
		assertEquals("unmarked", context.resolve("unmarked", Bean.class).name);
		assertEquals(asList(expected), acceptedInstances);
		assertEquals(1, acceptedMethods.size());
		assertEquals("myMethod", acceptedMethods.get(0).getName());
	}

	@Test
	public void interfacesCanBeUsedToMarkSetsOfTypes() {
		Env env = Environment.DEFAULT.with(DynamicLinker.class, (i,m) -> {});
		Injector context = Bootstrap.injector(env, TestMarkBindsModule.class);
		assertEquals(true, context.resolve("marked", Bean.class).cleaned,
				"clean was not called for marked bean");
		assertEquals(true, context.resolve("unmarked", Bean.class).cleaned,
				"clean was not called for unmarked bean");
	}
}
