package test.integration.contract;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.contract.ContractModule;
import se.jbee.inject.contract.UnboxingFuture;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.*;
import static test.integration.util.TestUtils.wait50;

/**
 * Most basic test for the bare functionality provided by the event system.
 *
 * This test only validates the expected output but does not care about the
 * processing restrictions that control correct processing of events with
 * regards to isolation/threading.
 */
class TestEventBasics {

	public interface Handler {

		void onChange(String msg);

		String compute(int x, int y);

		Future<String> computeEventually(int x);

	}

	public static class Service implements Handler {

		List<String> messages = new ArrayList<>();

		@Override
		public String compute(int x, int y) {
			return x + ":" + y;
		}

		@Override
		public Future<String> computeEventually(int x) {
			return completedFuture("42" + x);
		}

		@Override
		public void onChange(String msg) {
			messages.add(msg);
		}

	}

	private static class TestEventsModule extends ContractModule {

		@Override
		protected void declare() {
			handle(Handler.class);
			construct(Service.class);
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestEventsModule.class);

	@Test
	void thatNonReturnDispatchIsNonBlockingForTheCaller() {
		Handler proxy = injector.resolve(Handler.class);
		Service service = injector.resolve(Service.class);
		assertEquals(0, service.messages.size());
		proxy.onChange("foo");
		wait50(); // give time for the message to arrive
		assertEquals(1, service.messages.size());
		assertEquals("foo", service.messages.get(0));
		proxy.onChange("bar");
		wait50(); // give time for the message to arrive
		assertEquals(2, service.messages.size());
		assertEquals("bar", service.messages.get(1));
	}

	@Test
	void thatHandledInterfacesInjectProxy() {
		Handler proxy = injector.resolve(Handler.class);
		assertTrue(Proxy.isProxyClass(proxy.getClass()));
		assertSame(injector.resolve(Handler.class), proxy,
				"should be 'cached and reused'");
	}

	@Test
	void thatComputationDispatchWorks() {
		Handler proxy = injector.resolve(Handler.class);
		Service service = injector.resolve(Service.class); // need to exist
		assertNotNull(service);
		assertEquals("1:2", proxy.compute(1, 2));
		assertEquals("3:4", proxy.compute(3, 4));
		assertEquals("5:6", proxy.compute(5, 6));
	}

	@Test
	void thatEventualComputationDispatchWorks() {
		Handler proxy = injector.resolve(Handler.class);
		Service service = injector.resolve(Service.class); // need to exist
		assertNotNull(service);
		try {
			Future<String> fc = proxy.computeEventually(3);
			assertSame(UnboxingFuture.class, fc.getClass());
			assertEquals("423", fc.get());
		} catch (Exception e) {
			throw new AssertionError("Should have worked but got:", e);
		}

	}
}
