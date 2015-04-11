package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

import java.util.Properties;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BoundParameter;
import se.jbee.inject.config.Globals;
import se.jbee.inject.config.Presets;

/**
 * In reply to https://groups.google.com/forum/#!topic/silk-di/JhBnvF7k6Q4
 */
public class TestExample1Binds {

	static class MyClass {
		
		final String abc;
		final int twelve;
		
		public MyClass(String abc, int twelve) {
			super();
			this.abc = abc;
			this.twelve = twelve;
		}
		
	}
	
	static final class Example1Module1 extends BinderModuleWith<Properties> {

		@Override
		protected void declare(Properties properties) {
			bind(MyClass.class).toConstructor(
					BoundParameter.constant(String.class, properties.getProperty("x")),
					BoundParameter.constant(Integer.class, (Integer)properties.get("y")));
		}
	}
	
	static final class Example1Module2 extends BinderModule {

		@Override
		protected void declare() {
			bind(MyClass.class).toConstructor(
					instance(named("foo"), raw(String.class)),
					instance(named("bar"), raw(int.class))
					);
			// the below may of course appear in any other module
			bind(named("foo"), String.class).to("abc");
			bind(named("bar"), int.class).to(12);
		}
	}
	
	@Test
	public void constructorArgumentsCanBePassedToBootstrappingUsingPresets() {
		Properties props = new Properties();
		props.put("x", "abc");
		props.put("y", 12);
		Presets presets = Presets.EMPTY.preset(Properties.class, props);
		Globals globals = Globals.STANDARD.presets(presets);
		Injector injector = Bootstrap.injector(Example1Module1.class, globals);
		MyClass obj = injector.resolve(Dependency.dependency(MyClass.class));
		assertEquals(12, obj.twelve);
		assertEquals("abc", obj.abc);
	}
	
	@Test
	public void constructorArgumentsCanBeResolvedUsingNamedInstances() {
		Injector injector = Bootstrap.injector(Example1Module2.class);
		MyClass obj = injector.resolve(Dependency.dependency(MyClass.class));
		assertEquals(12, obj.twelve);
		assertEquals("abc", obj.abc);
	}
}
