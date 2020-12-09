package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Resource;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.ProducesBy;
import se.jbee.inject.lang.Type;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.lang.Type.raw;

class TestTypeVariableAutobindBinds {

	public static class TestTypeVariableAutobindBindsModule extends BinderModule {

		@Override
		protected void declare() {
			autobind().produceBy(ProducesBy.declaredMethods).in(this);
		}

		/**
		 * As the first parameter is the same {@link Type} as the return type of
		 * the this method the actual expected return type is injected. This
		 * allows to use methods with type variables as part of the dependency
		 * injection. The actual return type results from the type that is
		 * resolved by the {@link Injector}. If no {@link Resource} is bound to
		 * the exact match generic {@link Resource} like this method match.
		 */
		public <T> Function<T, String> injectsActualReturnType(
				Type<Function<T, String>> actualReturnType) {
			return val -> actualReturnType.toString() + ":" + val.toString();
		}
	}

	private final Injector context = Bootstrap.injector(
			TestTypeVariableAutobindBindsModule.class);

	@Test
	public void actualReturnTypeIsInjectedAsFirstArgumentIfParameterTypeMatchesTypeOfReturnType() {
		@SuppressWarnings("unchecked")
		Function<Integer, String> f = context.resolve(
				raw(Function.class).parametized(Integer.class, String.class));
		assertEquals(
				"java.util.function.Function<java.lang.Integer,java.lang.String>:42",
				f.apply(42));
	}

}
