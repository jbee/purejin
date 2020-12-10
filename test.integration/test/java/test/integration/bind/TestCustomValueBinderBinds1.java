package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.bind.*;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.New;
import se.jbee.inject.binder.Supply;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Environment;
import se.jbee.inject.defaults.DefaultValueBinders;
import se.jbee.inject.lang.Type;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.jbee.inject.bind.BindingType.CONSTRUCTOR;
import static se.jbee.inject.lang.Type.fieldType;

/**
 * Demonstrates how to use {@link ValueBinder}s to customize the and binding
 * automatics.
 *
 * In particular the {@link FieldInjectionBinder} shows how one could initialize
 * fields when an instance is created by using a custom macro that decorates the
 * original {@link Supplier}.
 *
 * The {@link RequiredConstructorParametersBinder} shows how all parameters of a
 * type bound to a constructor can add bindings that make the parameter's types
 * required so that eager exception occurs if no type is known for a parameter.
 */
class TestCustomValueBinderBinds1 {

	@Target({ METHOD, FIELD })
	@Retention(RUNTIME)
	public @interface Initialisation {

	}

	public static class Foo {

		@SuppressWarnings("unused")
		Foo(Integer i, Float f) {
			// no further usage
		}
	}

	public static class Bar {

		@Initialisation
		String s;
	}

	private static class TestCustomValueBinderBinds1Module
			extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("answer");
			bind(Integer.class).to(42);
			bind(Boolean.class).to(true);
			bind(Foo.class).toConstructor();
			bind(Number.class).to(Integer.class);
			bind(Bar.class).toConstructor();
		}
	}

	private static class EmptyModule extends BinderModule {

		@Override
		protected void declare() {
			// acts as a reference
		}

	}

	private static final class CountBinder implements ValueBinder<Binding<?>> {

		int expands = 0;

		CountBinder() {
			// make visible
		}

		@Override
		public <T> void expand(Env env, Binding<?> value, Binding<T> incomplete,
				Bindings bindings) {
			expands++;
		}

	}

	@Test
	void thatBindingsCanJustBeCounted() {
		CountBinder count = new CountBinder();
		CountBinder emptyCount = new CountBinder();
		Injector injector = injectorWithEnv(
				TestCustomValueBinderBinds1Module.class, count);
		injectorWithEnv(EmptyModule.class, emptyCount);
		assertEquals(0, injector.resolve(Resource[].class).length);
		assertEquals(6, count.expands - emptyCount.expands);
	}

	private static Injector injectorWithEnv(Class<? extends Bundle> root,
			ValueBinder<?> binder) {
		return Bootstrap.injector(Environment.DEFAULT.withBinder(binder), root);
	}

	/**
	 * A {@link ValueBinder} that add further bindings to make all types of used
	 * {@link Constructor} parameters {@link DeclarationType#REQUIRED}.
	 *
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	static final class RequiredConstructorParametersBinder
			implements ValueBinder<New<?>> {

		@Override
		public <T> void expand(Env env, New<?> value, Binding<T> incomplete,
				Bindings bindings) {
			DefaultValueBinders.NEW.expand(env, value, incomplete, bindings);
			Type<?>[] params = Type.parameterTypes(value.target);
			for (Type<?> param : params) {
				bindings.addExpanded(env, required(param, incomplete));
			}
		}

		private static <T> Binding<T> required(Type<T> type,
				Binding<?> binding) {
			return Binding.binding(new Locator<>(Instance.anyOf(type)),
					BindingType.REQUIRED, Supply.required(), binding.scope,
					binding.source.typed(DeclarationType.REQUIRED));
		}
	}

	@Test
	void thatAllConstructorParameterTypesCanBeMadeRequired() {
		ValueBinder<?> required = new RequiredConstructorParametersBinder();
		assertThrows(NoResourceForDependency.class,
				() -> injectorWithEnv(TestCustomValueBinderBinds1Module.class, required));
	}

	/**
	 * A simple example-wise {@link Supplier} that allows to initialised newly
	 * created instances.
	 *
	 * In this example a very basic field injection is build but it could be any
	 * kind of context dependent instance initialisation.
	 *
	 * @author Jan Bernitt (jan@jbee.se)
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
				if (f.isAnnotationPresent(Initialisation.class)) {
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
	 *
	 * @author Jan Bernitt (jan@jbee.se)
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
	void thatCustomInitialisationCanBeAdded() {
		Injector injector = injectorWithEnv(
				TestCustomValueBinderBinds1Module.class,
				new FieldInjectionBinder());
		assertEquals("answer", injector.resolve(Bar.class).s);
	}
}
