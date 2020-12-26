package test.integration.bind;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import se.jbee.inject.Hint;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.ProducesBy;
import se.jbee.inject.lang.Type;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

		public List<T> singletonList(T value) {
			return Collections.singletonList(value);
		}
	}

	public static class AnotherGenericBean<T> {

		final T value;

		public AnotherGenericBean(T value) {
			this.value = value;
		}
	}

	private static class TestFeatureGenericBeanBindsModule extends BinderModule {

		@Override
		protected void declare() {
			construct(AnotherGenericBean.class);

			autobind().produceBy(ProducesBy.declaredMethods(false)) //
					.in(Hint.relativeReferenceTo(raw(GenericBean.class)
							.parameterized(String.class)));
		}
	}

	private final Injector context = Bootstrap.injector(
			TestFeatureGenericBeanBindsModule.class);

	@Test
	void hintedConstructionForProductionOfGenericReturnType() {
		assertEquals("correct", context.resolve(String.class));
	}

	@Test
	void hintedConstructionForProductionOfComplexGenericReturnTypeAndGenericParameter() {
		assertEquals(singletonList("correct"), context.resolve(listTypeOf(String.class)));
	}

	@Disabled("Generic classes are not yet fully supported")
	@Test
	void adHocGenericConstruction() {
		AnotherGenericBean<?> bean = context.resolve(
				raw(AnotherGenericBean.class).parameterized(String.class));
		assertNotNull(bean);
		assertEquals("correct", bean.value);
	}
}
