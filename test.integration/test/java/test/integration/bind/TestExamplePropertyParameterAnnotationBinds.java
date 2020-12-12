package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModuleWith;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Environment;
import se.jbee.inject.config.HintsBy;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Type.parameterType;
import static se.jbee.inject.lang.Type.raw;

/**
 * This test illustrates how to extends the {@link Injector} so it does inject
 * {@link String} fields with a custom annotation ({@link Property}) referencing
 * a certain property value.
 * <p>
 * There are multiple ways to build this feature. In this example a custom
 * {@link HintsBy} strategy is used to read the {@link Property} annotation and
 * if present create a corresponding {@link Hint} which then causes the
 * injection of the instance that has the name given in the {@link Property}
 * annotation.
 * <p>
 * In this example the custom {@link HintsBy} strategy is only overridden for
 * the scope of the declaring {@link BinderModuleWith}. This could equally be
 * applied "globally" by setting it in the {@link Env} using {@link
 * Environment#with(Class, Object)} before the {@link Injector} is bootstrapped
 * from the {@link Env}.
 */
class TestExamplePropertyParameterAnnotationBinds {

	/**
	 * A custom annotation used to mark {@link String} parameters that should be
	 * injected with a certain property whose name is given by the annotation
	 * value.
	 */
	@Retention(RUNTIME)
	@Target({ PARAMETER, FIELD })
	@interface Property {
		String value();
	}

	private static class TestExamplePropertyParameterAnnotationBindsModule
			extends BinderModuleWith<Properties> {

		/**
		 * Configures custom {@link HintsBy} in the scope of this {@link
		 * BinderModuleWith}.
		 */
		@Override
		protected Env configure(Env env) {
			return Environment.override(env).with(HintsBy.class, this::hints);
		}

		/**
		 * The custom {@link HintsBy} implementation checks the the {@link
		 * Property} annotation; if present, a {@link Instance} hint is added to
		 * the constructor arguments linking the parameter to a named {@link
		 * String}. The name of that {@link String} is derived from the
		 * annotation value. The added {@link PropertySupplier} will be called
		 * when resolving the namespaced {@link String}. It extracts the
		 * property from the name and resolves it from the properties.
		 */
		private Hint<?>[] hints(Executable obj) {
			List<Hint<?>> hints = new ArrayList<>();
			for (java.lang.reflect.Parameter param : obj.getParameters()) {
				if (param.isAnnotationPresent(Property.class)) {
					Name name = named(Property.class).concat(
							param.getAnnotation(Property.class).value());
					Instance<?> hint = instance(name, parameterType(param));
					hints.add(hint.asHint());
				}
			}
			return hints.isEmpty() ? Hint.none() : hints.toArray(new Hint[0]);
		}

		@Override
		protected void declare(Properties properties) {
			bind(Properties.class).to(properties);
			per(Scope.dependencyInstance).bind(named(Property.class).asPrefix(),
					raw(String.class)).toSupplier(new PropertySupplier());
			// just to test
			bind(ExampleBean.class).toConstructor();
		}

	}

	static final class PropertySupplier implements Supplier<String> {

		@Override
		public String supply(Dependency<? super String> dep, Injector context)
				throws UnresolvableDependency {
			return context.resolve(Properties.class) //
					.getProperty(dep.instance.name.withoutNamespace());
		}
	}

	public static class ExampleBean {

		final String property1;
		final String property2;

		public ExampleBean(
				@Property("foo") String property1,
				@Property("bar") String property2) {
			this.property1 = property1;
			this.property2 = property2;
		}
	}

	@Test
	void customHintsAndSupplierCanBeUsedToBuildAnnotationBasedPropertyInjection() {
		Properties properties = new Properties();
		properties.put("foo", "property1");
		properties.put("bar", "property2");
		Env env = Environment.DEFAULT.with(Properties.class, properties);
		Injector context = Bootstrap.injector(env,
				TestExamplePropertyParameterAnnotationBindsModule.class);
		ExampleBean bean = context.resolve(ExampleBean.class);
		assertEquals("property1", bean.property1);
		assertEquals("property2", bean.property2);
	}
}
