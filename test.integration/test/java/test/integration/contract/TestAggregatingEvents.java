package test.integration.contract;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.contract.ContractModule;
import se.jbee.inject.contract.EventPolicy;
import se.jbee.inject.contract.EventPolicy.Flags;
import se.jbee.inject.contract.PolicyProvider;

import java.util.function.BinaryOperator;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Name.named;

class TestAggregatingEvents {

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

	private static class TestAggregatingEventsModule extends ContractModule {

		@Override
		protected void declare() {
			handle(Handler.class);
			bind(named("a"), Handler.class).to(new Service(true, 1));
			bind(named("b"), Handler.class).to(new Service(false, 2));
			bind(PolicyProvider.class).to(handler -> {
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

	@BeforeEach
	void preconditions() {
		assertSame(Service.class, a.getClass());
		assertSame(Service.class, b.getClass());
		assertNotSame(a, b);
		assertNotSame(Service.class, proxy.getClass());
	}

	@Test
	@Disabled("TODO #80 // TestAggregatingEvents.staticAggregationIsPerformed()")
	void staticAggregationIsPerformed() {
		assertFalse(proxy.all());
		assertEquals(3, proxy.sum());
	}

	@Test
	@Disabled("TODO #80 // TestAggregatingEvents.dynamicAggregationIsPerformed()")
	void dynamicAggregationIsPerformed() {
		assertTrue(proxy.dynamic(Boolean::logicalOr));
		assertEquals(5, proxy.dynamic(3, Integer::max));
	}
}
