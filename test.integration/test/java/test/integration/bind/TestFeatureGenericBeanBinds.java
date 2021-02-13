package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Hint;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.AccessesBy;
import se.jbee.inject.config.ProducesBy;
import se.jbee.lang.Type;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.jbee.lang.Cast.listTypeOf;
import static se.jbee.lang.Type.raw;

/**
 * A test that shows that class type variables can be used as long as actual
 * type parameters are set using {@link Hint}s or a {@link
 * se.jbee.inject.Dependency} with actual type parmaters.
 */
class TestFeatureGenericBeanBinds {

	@SuppressWarnings("unchecked")
	public static class GenericProduction<T> {

		final Type<GenericProduction<T>> type;

		public GenericProduction(Type<GenericProduction<T>> type) {
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

	public static class GenericConstruction<A, B> {

		final A a;
		final B b;

		public GenericConstruction(A a, B b) {
			this.a = a;
			this.b = b;
		}
	}

	public static class GenericAccess<T> {

		public final T value;

		public GenericAccess(T value) {
			this.value = value;
		}
	}

	private static class TestFeatureGenericBeanBindsModule extends BinderModule {

		@Override
		protected void declare() {
			construct(GenericConstruction.class);
			bind(Integer.class).to(42); // just a test value to inject

			autobind().produceBy(ProducesBy.declaredMethods(false)) //
					.in(Hint.relativeReferenceTo(raw(GenericProduction.class)
							.parameterized(String.class)));

			autobind().accessBy(AccessesBy.declaredFields(false)).in(
					Hint.constant(new GenericAccess<>(42d))
							.asType(raw(GenericAccess.class) //
									.parameterized(Double.class)));
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

	@Test
	void hintedAccessOfGenericFieldType() {
		assertEquals(42d, context.resolve(Double.class));
	}

	@Test
	void adHocGenericConstruction() {
		GenericConstruction<?, ?> bean = context.resolve(
				raw(GenericConstruction.class)
						.parameterized(String.class, Integer.class));
		assertNotNull(bean);
		assertEquals("correct", bean.a);
		assertEquals(42, bean.b);
	}

}
