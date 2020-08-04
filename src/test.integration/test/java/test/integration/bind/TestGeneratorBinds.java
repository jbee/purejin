package test.integration.bind;

import org.junit.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Generator;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Cast.resourceTypeFor;
import static se.jbee.inject.Type.raw;

public class TestGeneratorBinds {

	private static final class GeneratorBindsModule extends BinderModule
			implements Generator<String> {

		@Override
		protected void declare() {
			bind(String.class).toGenerator(this);
		}

		@Override
		public String generate(Dependency<? super String> dep)
				throws UnresolvableDependency {
			return "hello world";
		}
	}

	private final Injector injector = Bootstrap.injector(
			GeneratorBindsModule.class);

	@Test
	public void generatorCanBePassedDirectly() {
		assertEquals("hello world", injector.resolve(String.class));
		assertEquals("SupplierGeneratorBridge",
				injector.resolve(resourceTypeFor(raw(
						String.class))).generator.getClass().getSimpleName());
	}

}
