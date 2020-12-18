package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Generator;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Resource.resourceTypeOf;
import static se.jbee.inject.lang.Type.raw;

class TestBasicGeneratorBinds {

	private static final class TestBasicGeneratorBindsModule extends BinderModule
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
			TestBasicGeneratorBindsModule.class);

	@Test
	void generatorCanBePassedDirectly() {
		assertEquals("hello world", injector.resolve(String.class));
		assertEquals("SupplierGeneratorBridge",
				injector.resolve(resourceTypeOf(raw(
						String.class))).generator.getClass().getSimpleName());
	}
}
