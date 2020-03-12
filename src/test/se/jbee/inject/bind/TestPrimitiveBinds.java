package se.jbee.inject.bind;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Name.named;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * In Silk primitives and wrapper {@link Class}es are the same {@link Type}.
 *
 * @author Jan Bernitt (jan@jbee.se)
 *
 */
public class TestPrimitiveBinds {

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

	private static class PrimitiveBindsBean {

		final int i;
		final float f;
		final boolean b;
		final Integer bigI;
		final Float bigF;
		final Boolean bigB;

		@SuppressWarnings("unused")
		PrimitiveBindsBean(int i, float f, boolean b, Integer bigI, Float bigF,
				Boolean bigB) {
			this.i = i;
			this.f = f;
			this.b = b;
			this.bigI = bigI;
			this.bigF = bigF;
			this.bigB = bigB;
		}

	}

	private final Injector injector = Bootstrap.injector(
			PrimitiveBindsModule.class);

	@Test
	public void thatIntPrimitivesWorkAsWrapperClasses() {
		assertEquals(42, injector.resolve(Integer.class).intValue());
		assertEquals(42, injector.resolve(int.class).intValue());
	}

	@Test
	public void thatLongPrimitivesWorkAsWrapperClasses() {
		assertEquals(132L, injector.resolve(Long.class).longValue());
		assertEquals(132L, injector.resolve(long.class).longValue());
	}

	@Test
	public void thatBooleanPrimitivesWorkAsWrapperClasses() {
		assertEquals(true, injector.resolve(Boolean.class));
		assertEquals(true, injector.resolve(boolean.class));
	}

	@Test
	public void thatFloatPrimitivesWorkAsWrapperClasses() {
		assertEquals(3.1415f, injector.resolve("pi", Float.class).floatValue(),
				0.01f);
		assertEquals(3.1415f, injector.resolve("pi", float.class).floatValue(),
				0.01f);
	}

	@Test
	public void thatDoublePrimitivesWorkAsWrapperClasses() {
		assertEquals(2.71828d, injector.resolve("e", Double.class), 0.01d);
		assertEquals(2.71828d, injector.resolve("e", double.class), 0.01d);
	}

	/**
	 * To allow such a automatic conversion a bridge could be bound for each of
	 * the primitives. Such a bind would look like this.
	 *
	 * <pre>
	 * bind(int[].class).to(new IntToIntergerArrayBridgeSupplier());
	 * </pre>
	 *
	 * The stated bridge class is not a part of Silk but easy to do. Still a
	 * loot of code for all the primitives for a little benefit.
	 */
	@Test(expected = NoResourceForDependency.class)
	public void thatPrimitveArrayNotWorkAsWrapperArrayClasses() {
		assertArrayEquals(new int[42], injector.resolve(int[].class));
	}

	@Test
	public void thatPrimitivesWorkAsPrimitiveOrWrapperClassesWhenInjected() {
		PrimitiveBindsBean bean = injector.resolve(PrimitiveBindsBean.class);
		assertEquals(42, bean.i);
		assertEquals(3.1415f, bean.f, 0.01f);
		assertEquals(true, bean.b);
		assertEquals(42, bean.bigI.intValue());
		assertEquals(3.1415f, bean.bigF, 0.01f);
		assertEquals(true, bean.bigB);
	}
}
