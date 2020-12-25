package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Environment;
import se.jbee.inject.config.Config;
import se.jbee.inject.config.HintsBy;
import se.jbee.inject.config.NamesBy;
import se.jbee.inject.lang.Type;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.NoSuchElementException;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A test that show how to add an annotation (here {@link ConfigProperty} that
 * will inject the {@link se.jbee.inject.config.Config} value of the same name
 * to the annotated {@link java.lang.reflect.Parameter}.
 */
class TestExampleConfigPropertyAnnotationBinds {

	@Documented
	@Target(PARAMETER)
	@Retention(RUNTIME)
	@interface ConfigProperty {

		/**
		 * @return name of the property
		 */
		String value();
	}

	public static class Bean {

		final String a;
		final String b;

		public Bean(@ConfigProperty("a") String a, @ConfigProperty("b") String b) {
			this.a = a;
			this.b = b;
		}
	}

	private static class TestExampleConfigPropertyAnnotationBindsModule extends
			BinderModule {

		@Override
		protected void declare() {
			// some configuration value definitions
			config(Bean.class).bind("a", String.class).to("a");
			config(Bean.class).bind("b", String.class).to("b");

			// assume somewhere else (in another module) we declare
			construct(Bean.class);

			// and in some general setup module we declare the mechanism of
			// providing config values
			bindConfigPropertySupplier(Type.WILDCARD);
		}

		<T> void bindConfigPropertySupplier(Type<T> type) {
			per(Scope.dependencyInstance).bind(Name.named(ConfigProperty.class).asPrefix(),
					type).toSupplier(TestExampleConfigPropertyAnnotationBindsModule::supplyConfigProperty);
		}

		@SuppressWarnings("unchecked")
		static <T> T supplyConfigProperty(
				Dependency<? super T> dep, Injector context) {
			String property = dep.instance.name.toString().substring(
					Name.named(ConfigProperty.class).toString().length());
			Config config = context.resolve(dep.uninject().instanced(
					Instance.defaultInstanceOf(Type.raw(Config.class))));
			return (T) config.value(property).as(dep.type()).orElseThrow(
					() -> new UnresolvableDependency.SupplyFailed(
							"No such configuration value or converter",
							new NoSuchElementException()));
		}
	}

	private final Injector context = Bootstrap.injector(
			Environment.DEFAULT.with(HintsBy.class, HintsBy.instanceReference(
					NamesBy.annotatedWith(ConfigProperty.class,
							ConfigProperty::value, true))),
			TestExampleConfigPropertyAnnotationBindsModule.class);

	@Test
	void configPropertyIsInjectedAsNamedByTheAnnotation() {
		Bean bean = context.resolve(Bean.class);
		assertEquals("a", bean.a);
		assertEquals("b", bean.b);
	}
}
