package test.integration.contract;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.contract.ContractModule;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Name.named;

class TestRoundRobinComputeEvents {

	private interface Handler {

		int compute(int x);
	}

	private static final class Service implements Handler {

		private final int inc;

		public Service(int inc) {
			this.inc = inc;
		}

		@Override
		public int compute(int x) {
			return x + inc;
		}

	}

	private static final class TestRoundRobinComputeEventsModule
			extends ContractModule {

		@Override
		protected void declare() {
			handle(Handler.class);
			bind(named("a"), Service.class).to(new Service(5));
			bind(named("b"), Service.class).to(new Service(7));
			bind(named("c"), Service.class).to(new Service(3));
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestRoundRobinComputeEventsModule.class);

	@Test
	@Disabled("TODO #80 // TestRoundRobinComputeEvents.computationUsesAllAvailableServices()")
	void computationUsesAllAvailableServices() {
		Handler h = injector.resolve(Handler.class);
		Service a = injector.resolve("a", Service.class);
		Service b = injector.resolve("b", Service.class);
		Service c = injector.resolve("c", Service.class);

		assertNotNull(a);
		assertNotNull(b);
		assertNotNull(c);
		assertNotSame(a, b);
		assertNotSame(b, c);
		assertNotSame(a, c);

		int sum = 0;
		sum += h.compute(1);
		sum += h.compute(1);
		sum += h.compute(1);
		assertEquals(18, sum); // 5+1 + 7+1 + 3+1 = 18 (no guarantee for order)

		// next round
		sum += h.compute(3);
		sum += h.compute(3);
		sum += h.compute(3); // 5+3 + 7+3 + 3+3 = 24 (no guarantee for order)
		assertEquals(42, sum); // 18 + 24 = 42
	}
}
