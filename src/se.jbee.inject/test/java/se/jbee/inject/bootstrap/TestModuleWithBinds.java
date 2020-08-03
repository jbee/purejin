package se.jbee.inject.bootstrap;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static se.jbee.inject.Cast.listTypeOf;
import static se.jbee.inject.Name.named;

import java.util.List;
import java.util.Properties;

import org.junit.Test;

import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.binder.BinderModuleWith;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Environment;

/**
 * This test demonstrates how to set properties in the {@link Env} using the
 * {@link Environment} utility. The value passed into
 * {@link BinderModuleWith#declare(Object)} is determined by the type of the
 * generic. This has to be the same {@link Type} as the one used when declaring
 * the value using {@link Environment#with(Type, Object)}.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestModuleWithBinds {

	private static class TestModuleWithBindsBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(TestModuleWithBindsModule1.class);
			install(TestModuleWithBindsModule2.class);
			install(TestModuleWithBindsModule3.class);
		}
	}

	private static class TestModuleWithBindsModule1
			extends BinderModuleWith<Properties> {

		@Override
		protected void declare(Properties property) {
			bind(named("foo"), String.class).to(
					property.getProperty("foo.text"));
		}
	}

	private static class TestModuleWithBindsModule2
			extends BinderModuleWith<List<String>> {

		@Override
		protected void declare(List<String> property) {
			bind(named("list"), String.class).to(property.get(1));
		}

	}

	private static class TestModuleWithBindsModule3
			extends BinderModuleWith<List<Integer>> {

		@Override
		protected void declare(List<Integer> property) {
			bind(named("list"), Integer.class).to(property.get(1));
		}

	}

	private static class TestModuleWithBindsModule4
			extends BinderModuleWith<Env> {

		@Override
		protected void declare(Env property) {
			assertNotNull(property);
		}

	}

	private final Injector injector = injector();

	private static Injector injector() {
		Env env = Bootstrap.ENV //
				.with(Properties.class, exampleProperties()) //
				.with(listTypeOf(String.class), asList("a", "b")) //
				.with(listTypeOf(Integer.class), asList(1, 2));
		return Bootstrap.injector(env, TestModuleWithBindsBundle.class);
	}

	private static Properties exampleProperties() {
		Properties props = new Properties();
		props.put("foo.text", "bar");
		return props;
	}

	@Test
	public void thatPresetValuePassedToModule() {
		assertEquals("bar", injector.resolve("foo", String.class));
	}

	@Test
	public void thatDifferentParametizedPresetValuesForSameGenericTypeArePosssible() {
		assertEquals("b", injector.resolve("list", String.class));
		assertEquals(2, injector.resolve("list", Integer.class).intValue());
	}

	@Test
	public void envItselfCanBePassedToModule() {
		assertNotNull(Bootstrap.injector(TestModuleWithBindsModule4.class));
	}
}
