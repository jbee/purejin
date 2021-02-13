package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.bind.Bindings;
import se.jbee.inject.binder.BinderModuleWith;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.lang.Type;

import java.util.List;
import java.util.Properties;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Cast.listTypeOf;

/**
 * This test demonstrates how to set properties in the {@link Env} using the
 * {@link Env#with(Name, Type, Object)} utility.
 * <p>
 * The bound {@link Env} properties are passed into {@link BinderModuleWith} as
 * argument based on their specific type parameter to {@link BinderModuleWith}'s
 * type variable. The generic type used is the one resolved from the {@link Env}
 * and that is bound in setup using {@link Env#with(Type, Object)}.
 * <p>
 * A look into the implementation of {@link se.jbee.inject.bind.ModuleWith#declare(Bindings,
 * Env)} shows that this is just a convenience functionality build upon a
 * convention which can easily be extended to more than one property.
 */
class TestBasicModuleWithBinds {

	private static class TestBasicModuleWithBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(TestBasicModuleWithBindsModule1.class);
			install(TestBasicModuleWithBindsModule2.class);
			install(TestBasicModuleWithBindsModule3.class);
		}
	}

	private static class TestBasicModuleWithBindsModule1
			extends BinderModuleWith<Properties> {

		@Override
		protected void declare(Properties property) {
			bind(named("foo"), String.class).to(
					property.getProperty("foo.text"));
		}
	}

	private static class TestBasicModuleWithBindsModule2
			extends BinderModuleWith<List<String>> {

		@Override
		protected void declare(List<String> property) {
			bind(named("list"), String.class).to(property.get(1));
		}

	}

	private static class TestBasicModuleWithBindsModule3
			extends BinderModuleWith<List<Integer>> {

		@Override
		protected void declare(List<Integer> property) {
			bind(named("list"), Integer.class).to(property.get(1));
		}

	}

	private static class TestBasicModuleWithBindsModule4
			extends BinderModuleWith<Env> {

		@Override
		protected void declare(Env property) {
			assertNotNull(property);
		}

	}

	private final Injector context = injector();

	private static Injector injector() {
		Env env = Bootstrap.DEFAULT_ENV //
				.with(Properties.class, exampleProperties()) //
				.with(listTypeOf(String.class), asList("a", "b")) //
				.with(listTypeOf(Integer.class), asList(1, 2));
		return Bootstrap.injector(env, TestBasicModuleWithBindsBundle.class);
	}

	private static Properties exampleProperties() {
		Properties props = new Properties();
		props.put("foo.text", "bar");
		return props;
	}

	@Test
	void presetValuePassedToModule() {
		assertEquals("bar", context.resolve("foo", String.class));
	}

	@Test
	void fullGenericTypeIsConsideredWhenExtractingEnvValue() {
		assertEquals("b", context.resolve("list", String.class));
		assertEquals(2, context.resolve("list", Integer.class).intValue());
	}

	@Test
	void envItselfCanBePassedToModule() {
		assertNotNull(Bootstrap.injector(TestBasicModuleWithBindsModule4.class));
	}
}
