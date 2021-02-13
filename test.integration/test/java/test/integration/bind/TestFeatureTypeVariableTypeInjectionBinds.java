package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Resource;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.lang.Type;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.config.ProducesBy.declaredMethods;
import static se.jbee.lang.Cast.functionTypeOf;

/**
 * Tests the very special feature that injects the actual {@link Type} as 1st
 * argument in the presents of a type variable so that the implementation can
 * use the information about the actual type to compute its result.
 * <p>
 * This is the fully generic version of what is often found in java application
 * where {@link Class} references are passed on so that the receiver can perform
 * its operation based on that.
 */
class TestFeatureTypeVariableTypeInjectionBinds {

	public static class TestFeatureTypeVariableTypeInjectionBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			autobind().produceBy(declaredMethods(false)).in(this);
		}

		/**
		 * As the first parameter is the of type {@link Type} with the type
		 * arguments being the same as the return type of the the method the
		 * actual expected return type is injected. This allows to use methods
		 * with type variables as part of the dependency injection. The actual
		 * return type results from the type that is resolved by the {@link
		 * Injector}. If no {@link Resource} is bound to the exact match generic
		 * {@link Resource} like this method match.
		 */
		public <T> Function<T, String> injectsActualReturnType(
				Type<Function<T, String>> actualReturnType) {
			return val -> actualReturnType.toString() + ":" + val.toString();
		}
	}

	private final Injector context = Bootstrap.injector(
			TestFeatureTypeVariableTypeInjectionBindsModule.class);

	@Test
	void actualReturnTypeIsInjectedAsFirstArgumentIfParameterTypeMatchesTypeOfReturnType() {
		Function<Integer, String> f = context.resolve(
				functionTypeOf(Integer.class, String.class));
		assertEquals(
				"java.util.function.Function<java.lang.Integer,java.lang.String>:42",
				f.apply(42));
	}

}
