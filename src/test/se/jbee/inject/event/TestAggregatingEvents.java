package se.jbee.inject.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Name.named;

import java.util.function.BinaryOperator;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.event.EventPolicy.Flags;

public class TestAggregatingEvents {

	interface Handler {

		boolean all();

		boolean dynamic(BinaryOperator<Boolean> agg);

		int sum();

		int dynamic(int val, BinaryOperator<Integer> agg);
	}

	static class Service implements Handler {

		final boolean success;
		final int adds;

		Service(boolean success, int adds) {
			this.success = success;
			this.adds = adds;
		}

		@Override
		public boolean all() {
			return success;
		}

		@Override
		public boolean dynamic(BinaryOperator<Boolean> agg) {
			return success;
		}

		@Override
		public int sum() {
			return adds;
		}

		@Override
		public int dynamic(int val, BinaryOperator<Integer> agg) {
			return val + adds;
		}

	}

	private static class TestAggregatingEventsModule extends EventModule {

		@Override
		protected void declare() {
			handle(Handler.class);
			bind(named("a"), Handler.class).to(new Service(true, 1));
			bind(named("b"), Handler.class).to(new Service(false, 2));
			bind(EventMirror.class).to(handler -> {
				return EventPolicy.DEFAULT.with(
						Flags.MULTI_DISPATCH_AGGREGATED);
			});
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestAggregatingEventsModule.class);

	private final Handler a = injector.resolve(named("a"), Handler.class);
	private final Handler b = injector.resolve(named("b"), Handler.class);
	private final Handler proxy = injector.resolve(Handler.class);

	@Before
	public void preconditions() {
		assertSame(Service.class, a.getClass());
		assertSame(Service.class, b.getClass());
		assertNotSame(a, b);
		assertNotSame(Service.class, proxy.getClass());
	}

	@Test
	@Ignore("TODO #80 // TestAggregatingEvents.staticAggregationIsPerformed()")
	public void staticAggregationIsPerformed() {
		assertFalse(proxy.all());
		assertEquals(3, proxy.sum());
	}

	@Test
	@Ignore("TODO #80 // TestAggregatingEvents.dynamicAggregationIsPerformed()")
	public void dynamicAggregationIsPerformed() {
		assertTrue(proxy.dynamic(Boolean::logicalOr));
		assertEquals(5, proxy.dynamic(3, Integer::max));
	}
}
