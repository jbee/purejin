package se.jbee.inject.event;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;

public class TestEvents {

	static interface MyListener {

		void onChange(String msg);
		
		String compute(int x, int y);
		
		Future<String> computeEventually(int x);
		
	}
	
	static class MyService implements MyListener {

		List<String> messages = new ArrayList<>();
		
		@Override
		public String compute(int x, int y) {
			return x + ":" + y;
		}

		@Override
		public Future<String> computeEventually(int x) {
			return completedFuture("42"+x);
		}
		
		@Override
		public void onChange(String msg) {
			messages.add(msg);
		}
		
	}
	
	private static class TestEventsModule extends EventModule {

		@Override
		protected void declare() {
			handle(MyListener.class);
			construct(MyService.class);
		}
	}
	
	private final Injector injector = Bootstrap.injector(TestEventsModule.class);
	
	@Test
	public void thatNonReturnDisptachIsNonBlockingForTheCaller() throws InterruptedException {
		MyListener proxy = injector.resolve(MyListener.class);
		MyService service = injector.resolve(MyService.class);
		proxy.onChange("foo");
		assertEquals(service.messages.size(), 0); // this might fail if the processing was very fast
		Thread.sleep(50); // give time for the message to arrive
		assertEquals(1, service.messages.size());
		assertEquals("foo", service.messages.get(0));
		proxy.onChange("bar");
		Thread.sleep(50); // give time for the message to arrive
		assertEquals(2, service.messages.size());
		assertEquals("bar", service.messages.get(1));
	}
	
	@Test
	public void thatHandledInterfacesInjectProxy() {
		MyListener proxy = injector.resolve(MyListener.class);
		assertTrue(Proxy.isProxyClass(proxy.getClass()));
		assertSame(injector.resolve(MyListener.class), proxy); // should be "cached and reused"
	}
	
	@Test
	public void thatComputationDispatchWorks() {
		MyListener proxy = injector.resolve(MyListener.class);
		MyService service = injector.resolve(MyService.class); // need to exist
		assertEquals("1:2", proxy.compute(1, 2));
		assertEquals("3:4", proxy.compute(3, 4));
		assertEquals("5:6", proxy.compute(5, 6));
	}
	
	@Test
	public void thatEventualComputationDispatchWorks() {
		MyListener proxy = injector.resolve(MyListener.class);
		MyService service = injector.resolve(MyService.class); // need to exist
		try {
			Future<String> fc = proxy.computeEventually(3);
			assertSame(UnboxingFuture.class, fc.getClass());
			assertEquals("423", fc.get());
		} catch (Exception e) {
			fail("Should work");
		}

	}
}
