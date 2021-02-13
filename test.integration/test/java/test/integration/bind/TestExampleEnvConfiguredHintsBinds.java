package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Hint;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BinderModuleWith;
import se.jbee.inject.bootstrap.Bootstrap;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.raw;

/**
 * In reply to https://groups.google.com/forum/#!topic/silk-di/JhBnvF7k6Q4
 */
class TestExampleEnvConfiguredHintsBinds {

	public static class MyClass {

		final String abc;
		final int twelve;

		public MyClass(String abc, int twelve) {
			this.abc = abc;
			this.twelve = twelve;
		}

	}

	static final class Example1Module1 extends BinderModuleWith<Properties> {

		@Override
		protected void declare(Properties properties) {
			bind(MyClass.class).toConstructor(
					Hint.constant(properties.getProperty("x")),
					Hint.constant((Integer) properties.get("y")));
		}
	}

	static final class Example1Module2 extends BinderModule {

		@Override
		protected void declare() {
			bind(MyClass.class).toConstructor(
					instance(named("foo"), raw(String.class)).asHint(),
					instance(named("bar"), raw(int.class)).asHint());
			// the below may of course appear in any other module
			bind(named("foo"), String.class).to("abc");
			bind(named("bar"), int.class).to(12);
		}
	}

	@Test
	void passingPropertiesConfigurationToConfigureHints() {
		Properties props = new Properties();
		props.put("x", "abc");
		props.put("y", 12);
		Env env = Bootstrap.DEFAULT_ENV.with(Properties.class, props);
		Injector injector = Bootstrap.injector(env, Example1Module1.class);
		MyClass obj = injector.resolve(MyClass.class);
		assertEquals(12, obj.twelve);
		assertEquals("abc", obj.abc);
	}

	@Test
	void injectNamedInstancesAsConstructorArguments() {
		Injector injector = Bootstrap.injector(Example1Module2.class);
		MyClass obj = injector.resolve(MyClass.class);
		assertEquals(12, obj.twelve);
		assertEquals("abc", obj.abc);
	}
}
