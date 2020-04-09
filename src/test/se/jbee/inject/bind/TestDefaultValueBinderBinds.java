package se.jbee.inject.bind;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toCollection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.config.ProducesBy.allMethods;
import static se.jbee.inject.config.SharesBy.allFields;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.container.Supplier;
import se.jbee.inject.scope.ApplicationScope;
import se.jbee.inject.scope.ThreadScope;
import se.jbee.inject.scope.WorkerScope;

/**
 * Tests that illustrates that {@link Constructor}s and {@link Method}s bound
 * are not just used within the actual {@link Supplier}s but also accessible
 * directly by resolving a {@link Constructor} or {@link Method} array.
 */
public class TestDefaultValueBinderBinds {

	@Target({ METHOD, FIELD })
	@Retention(RUNTIME)
	static @interface Produces {

	}

	static class Example {

		@Produces
		static final Float CONSTANT = 42.0f;

		private String value;

		Example(String value) {
			this.value = value;
		}

		@Produces
		String getValue() {
			return value;
		}

		@Produces
		int getValueLength() {
			return value.length();
		}
	}

	static class TestDefaultValueBinderBindsModule extends BinderModule {

		@Override
		protected void declare() {
			injectingInto(Example.class).bind(String.class).to("constant");
			// use mirrors and an annotation to bind methods as Supplier
			autobind() //
					.produceBy(allMethods.annotatedWith(Produces.class)) //
					.shareBy(allFields.annotatedWith(Produces.class)) //
					.in(Example.class);
		}

	}

	private final Injector injector = Bootstrap.injector(
			TestDefaultValueBinderBindsModule.class);

	/**
	 * This isn't what this test wants to verify but this verifies the setup
	 * does the expected thing: Create an {@link Example}, inject the
	 * {@link String} "constant" and make both {@link Method}s annotated with
	 * {@link Produces} available as {@link String} and {@link Integer}.
	 */
	@Before
	public void preCondition() {
		assertNotNull(injector.resolve(Example.class));
		assertEquals("constant", injector.resolve(String.class));
		assertEquals("constant".length(),
				injector.resolve(int.class).intValue());
	}

	@Test
	public void valueBinderNewBindsConstructor() {
		Constructor<?>[] constructors = injector.resolve(Constructor[].class);
		assertTrue(constructors.length > 0);
		assertNoDuplicates(constructors);
		assertDeclaringClasses(constructors, Example.class, // 2 Scopes from core...
				ThreadScope.class, ApplicationScope.class, WorkerScope.class);
	}

	@Test
	public void valueBinderProducesBindsMethod() {
		Method[] methods = injector.resolve(Method[].class);
		assertTrue(methods.length > 0);
		assertNoDuplicates(methods);
		assertDeclaringClasses(methods, Example.class);
	}

	@Test
	public void valueBinderSharesBindsField() {
		Field[] constants = injector.resolve(Field[].class);
		assertTrue(constants.length > 0);
		assertNoDuplicates(constants);
		assertDeclaringClasses(constants, Example.class);
	}

	private static void assertDeclaringClasses(Member[] actual,
			Class<?>... expected) {
		assertEquals(new HashSet<>(asList(expected)),
				asList(actual).stream().map(e -> e.getDeclaringClass()).collect(
						toCollection(HashSet::new)));
	}

	private static <T> void assertNoDuplicates(T[] actual) {
		assertEquals(actual.length, new HashSet<>(asList(actual)).size());
	}
}
