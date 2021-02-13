package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.DeclarationType;
import se.jbee.inject.Env;
import se.jbee.inject.Instance;
import se.jbee.inject.Locator;
import se.jbee.inject.UnresolvableDependency.ResourceResolutionFailed;
import se.jbee.inject.bind.Binding;
import se.jbee.inject.bind.BindingType;
import se.jbee.inject.bind.Bindings;
import se.jbee.inject.bind.ValueBinder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Constructs;
import se.jbee.inject.binder.Supply;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.DefaultValueBinders;
import se.jbee.lang.Type;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.jbee.inject.bind.ValueBinder.valueBinderTypeOf;

/**
 * An example of how to use {@link ValueBinder}s to customize the and binding
 * process.
 *
 * The {@link RequiredConstructorParametersBinder} shows how all parameters of a
 * type bound to a constructor can add bindings that make the parameter's types
 * required so that eager exception occurs if no type is known for a parameter.
 *
 * For more examples on {@link ValueBinder}s have a look at:
 *
 * @see TestExampleCountBindingsBinds
 * @see TestExampleFieldInjectionBinds
 * @see TestExamplePreferConstantsBinds
 */
class TestExampleRequireConstructorParametersBinds {

	public static class RequirementsMissing {

		@SuppressWarnings("unused")
		RequirementsMissing(Integer i, Float f) {
			// no further usage
		}
	}

	private static class TestExampleRequireConstructorParametersBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("answer");
			bind(Integer.class).to(42);
			bind(Boolean.class).to(true);
			bind(Number.class).to(Integer.class);
			bind(RequirementsMissing.class).toConstructor();
		}
	}

	/**
	 * A {@link ValueBinder} that add further bindings to make all types of used
	 * {@link Constructor} parameters {@link DeclarationType#REQUIRED}.
	 */
	static final class RequiredConstructorParametersBinder
			implements ValueBinder<Constructs<?>> {

		@Override
		public <T> void expand(Env env, Constructs<?> ref, Binding<T> item,
				Bindings dest) {
			DefaultValueBinders.CONSTRUCTS.expand(env, ref, item, dest);
			Type<?>[] params = Type.parameterTypes(ref.target);
			for (Type<?> param : params) {
				dest.addExpanded(env, required(param, item));
			}
		}

		private static <T> Binding<T> required(Type<T> type,
				Binding<?> item) {
			return Binding.binding(new Locator<>(Instance.anyOf(type)),
					BindingType.REQUIRED, Supply.required(), item.scope,
					item.source.typed(DeclarationType.REQUIRED));
		}
	}

	@Test
	void missingBindingForFloatConstructorParameterCausesEagerExceptionDuringBootstrapping() {
		Class<TestExampleRequireConstructorParametersBindsModule> root = TestExampleRequireConstructorParametersBindsModule.class;
		ValueBinder<Constructs<?>> required = new RequiredConstructorParametersBinder();
		Exception ex = assertThrows(ResourceResolutionFailed.class,
				() -> Bootstrap.injector(Bootstrap.DEFAULT_ENV.with(
						valueBinderTypeOf(Constructs.class), required), root));
		assertEquals(
				"No resource for type(s)\n" + "\trequired: [java.lang.Float]",
				ex.getMessage());
	}

}
