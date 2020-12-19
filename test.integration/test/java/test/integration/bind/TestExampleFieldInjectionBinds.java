package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Supplier;
import se.jbee.inject.bind.Binding;
import se.jbee.inject.bind.Bindings;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.ValueBinder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.New;
import se.jbee.inject.binder.Supply;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Environment;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.bind.BindingType.CONSTRUCTOR;
import static se.jbee.inject.lang.Type.fieldType;

/**
 * An example of how to use {@link ValueBinder}s to customize the and binding
 * process.
 * <p>
 * By default the {@link Injector} expects constructor injection. It does
 * neither field nor setter injection as these lead to problematic code
 * patterns. This example shows how field injection could be added anyhow.
 * <p>
 * The {@link FieldInjectionBinder} shows how one could inject fields when an
 * instance is created by using a custom {@link ValueBinder} that decorates the
 * original {@link Supplier} to also inject fields.
 * <p>
 * It should be noted that this is just one possible way to add a feature like
 * field injection. Another solution would be the use of {@link
 * se.jbee.inject.BuildUp}s.
 * <p>
 * For more examples on {@link ValueBinder}s have a look at:
 *
 * @see TestExampleCountBindingsBinds
 * @see TestExampleRequireConstructorParametersBinds
 * @see TestExamplePreferConstantsBinds
 */
class TestExampleFieldInjectionBinds {

	@Target({ METHOD, FIELD })
	@Retention(RUNTIME)
	public @interface Inject {

	}

	public static class Bean {

		@Inject
		String s;
	}

	private static class TestExampleFieldInjectionBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("answer");
			bind(Bean.class).toConstructor();
		}
	}

	/**
	 * A simple example-wise {@link Supplier} that allows to initialised newly
	 * created instances.
	 *
	 * In this example a very basic field injection is build but it could be any
	 * kind of context dependent instance initialisation.
	 */
	static final class FieldInjectionSupplier<T> implements Supplier<T> {

		private final Supplier<T> decorated;

		FieldInjectionSupplier(Supplier<T> decorated) {
			this.decorated = decorated;
		}

		@Override
		public T supply(Dependency<? super T> dep, Injector context) {
			T instance = decorated.supply(dep, context);
			for (Field f : instance.getClass().getDeclaredFields()) {
				if (f.isAnnotationPresent(Inject.class)) {
					try {
						f.set(instance, context.resolve(fieldType(f)));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
			return instance;
		}
	}

	/**
	 * Decorates the usual constructor with initialization.
	 */
	static final class FieldInjectionBinder implements ValueBinder<New<?>> {

		@Override
		public <T> void expand(Env env, New<?> constructor,
				Binding<T> incomplete, Bindings bindings) {
			env.accessible(constructor.target);
			Supplier<T> supplier = new FieldInjectionSupplier<>(
					Supply.byNew(constructor.typed(incomplete.type())));
			bindings.addExpanded(env,
					incomplete.complete(CONSTRUCTOR, supplier));
		}
	}

	@Test
	void fieldInjectionTookPlace() {
		Injector injector = injectorWithEnv(
				TestExampleFieldInjectionBindsModule.class,
				new FieldInjectionBinder());
		assertEquals("answer", injector.resolve(Bean.class).s);
	}

	private static Injector injectorWithEnv(Class<? extends Bundle> root,
			ValueBinder<?> binder) {
		return Bootstrap.injector(Environment.DEFAULT.withBinder(binder), root);
	}
}