package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Hint;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.ProducesBy;
import se.jbee.inject.lang.Type;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.lang.Cast.listTypeOf;
import static se.jbee.inject.lang.Type.raw;

class TestFeatureGenericBeanBinds {

	@SuppressWarnings("unchecked")
	public static class GenericBean<T> {

		final Type<GenericBean<T>> type;

		public GenericBean(Type<GenericBean<T>> type) {
			this.type = type;
		}

		public T make() {
			if (type.parameter(0).rawType == String.class)
				return (T) "correct";
			return null;
		}

		public List<T> makeList() {
			if (type.parameter(0).rawType == String.class)
				return (List<T>) asList("a", "b");
			return null;
		}
	}

	private static class TestFeatureGenericBeanBindsModule extends BinderModule {

		@Override
		protected void declare() {
			autobind().produceBy(ProducesBy.declaredMethods(false)) //
					.in(Hint.relativeReferenceTo(raw(GenericBean.class)
							.parameterized(String.class)));
		}
	}

	private final Injector context = Bootstrap.injector(
			TestFeatureGenericBeanBindsModule.class);

	@Test
	void plainGenericReturnType() {
		assertEquals("correct", context.resolve(String.class));
	}

	@Test
	void complexGenericReturnType() {
		assertEquals(asList("a", "b"), context.resolve(listTypeOf(String.class)));
	}
}
