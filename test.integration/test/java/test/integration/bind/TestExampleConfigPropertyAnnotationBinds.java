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
import java.lang.reflect.AnnotatedElement;
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

		/**
		 * @return the type the property value was defined as
		 */
		Class<?> from() default String.class;
	}

	public static class Bean {

		final String a;
		final String b;

		public Bean(@ConfigProperty("a") String a,
				@ConfigProperty(value = "b", from = Integer.class) String b) {
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
			config(Bean.class).bind("b", Integer.class).to(42);

			// assume somewhere else (in another module) we declare
			construct(Bean.class);

			// and in some general setup module we declare the mechanism of
			// providing config values
			bindConfigPropertySupplier(Type.WILDCARD);

			// and also we need a converter Integer => String
			bind(Converter.converterTypeOf(Integer.class, String.class)).to(String::valueOf);
		}

		<T> void bindConfigPropertySupplier(Type<T> type) {
			per(Scope.dependencyInstance) //
					.bind(Name.named(ConfigProperty.class).asPrefix(), type) //
					.toSupplier(TestExampleConfigPropertyAnnotationBindsModule::supplyConfigProperty);
		}

		@SuppressWarnings("unchecked")
		static <T> T supplyConfigProperty(
				Dependency<? super T> dep, Injector context) {
			AnnotatedElement e = dep.at().element();
			ConfigProperty property = e.getAnnotation(ConfigProperty.class);
			Config config = context.resolve(dep.uninject().onInstance(
					Instance.defaultInstanceOf(Type.raw(Config.class))));
			return (T) config.value(property.from(), property.value())
					.as(dep.type()).orElseThrow(
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
	void configPropertyIsInjectedAsAskedByTheAnnotation() {
		assertEquals("a", context.resolve(Bean.class).a);
	}

	@Test
	void configPropertyIsInjectedAndConvertedAsAskedByTheAnnotation() {
		assertEquals("42", context.resolve(Bean.class).b);
	}
}
