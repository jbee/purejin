package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency.ResourceResolutionFailed;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.DefaultFeature;
import se.jbee.lang.Type;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Name.named;

/**
 * Primitives and wrapper {@link Class}es are the same {@link Type} as far as
 * the {@link Injector} is concerned.
 */
class TestBasicPrimitiveValueBinds {

	private static class PrimitiveBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(int.class).to(42);
			bind(boolean.class).to(true);
			bind(long.class).to(132L);
			bind(named("pi"), float.class).to(3.1415f);
			bind(named("e"), double.class).to(2.71828d);
			bind(PrimitiveBindsBean.class).toConstructor();
		}
	}

	public static class PrimitiveBindsBean {

		final int i;
		final float f;
		final boolean b;
		final Integer bigI;
		final Float bigF;
		final Boolean bigB;

		public PrimitiveBindsBean(int i, float f, boolean b, Integer bigI,
				Float bigF, Boolean bigB) {
			this.i = i;
			this.f = f;
			this.b = b;
			this.bigI = bigI;
			this.bigF = bigF;
			this.bigB = bigB;
		}
	}

	private final Injector context = Bootstrap.injector(
			PrimitiveBindsModule.class);

	@Test
	void intPrimitivesWorkAsWrapperClasses() {
		assertEquals(42, context.resolve(Integer.class).intValue());
		assertEquals(42, context.resolve(int.class).intValue());
	}

	@Test
	void longPrimitivesWorkAsWrapperClasses() {
		assertEquals(132L, context.resolve(Long.class).longValue());
		assertEquals(132L, context.resolve(long.class).longValue());
	}

	@Test
	void booleanPrimitivesWorkAsWrapperClasses() {
		assertEquals(true, context.resolve(Boolean.class));
		assertEquals(true, context.resolve(boolean.class));
	}

	@Test
	void floatPrimitivesWorkAsWrapperClasses() {
		assertEquals(3.1415f, context.resolve("pi", Float.class), 0.01f);
		assertEquals(3.1415f, context.resolve("pi", float.class), 0.01f);
	}

	@Test
	void doublePrimitivesWorkAsWrapperClasses() {
		assertEquals(2.71828d, context.resolve("e", Double.class), 0.01d);
		assertEquals(2.71828d, context.resolve("e", double.class), 0.01d);
	}

	/**
	 * To allow such a automatic conversion a bridge can be installed using
	 * {@link DefaultFeature#PRIMITIVE_ARRAYS}. By default
	 * however this is not installed so the below call fails.
	 */
	@Test
	void primitiveArrayNotWorkAsWrapperArrayClasses() {
		assertThrows(ResourceResolutionFailed.class,
				() -> context.resolve(int[].class));
	}

	@Test
	void primitivesWorkAsPrimitiveOrWrapperClassesWhenInjected() {
		PrimitiveBindsBean bean = context.resolve(PrimitiveBindsBean.class);
		assertEquals(42, bean.i);
		assertEquals(3.1415f, bean.f, 0.01f);
		assertTrue(bean.b);
		assertEquals(42, bean.bigI.intValue());
		assertEquals(3.1415f, bean.bigF, 0.01f);
		assertEquals(true, bean.bigB);
	}
}
