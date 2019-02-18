package se.jbee.inject.event;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * Tests to verify the correctness of the exception handling caused by timeouts
 * related to the {@link EventProperties#ttl} period.
 * 
 * The general sentiment is that the event system should behave as transparent
 * as possible. That means if the original handler method throws exceptions
 * these are observed as if the implementing method would have been called
 * directly. Similarly if the handler method signature allows for
 * {@link TimeoutException} to be thrown the timeout caused by expired TTL is
 * made visiable as {@link TimeoutException}. If the signature does not permit
 * it the timeout becomes visible as {@link EventException} which is caused by a
 * {@link TimeoutException}. This idea also covers eventually returned
 * {@link Future}s. If {@link Future#get(long, TimeUnit)} is used (which allows
 * {@link TimeoutException}) a {@link TimeoutException} is made visible directly
 * while {@link Future#get()} uses {@link EventException} caused by
 * {@link TimeoutException}.
 * 
 * The tests simulate timeout by implementing all {@link Handler} methods with
 * {@link Thread#sleep(long)} of 20ms and using a {@link ExecutorService} with a
 * single thread. Thereby a already running event will stop further processing
 * of other events for about 20ms. This gives a window in which further added
 * events (calls) can time out. The TTL is set to just 5ms which causes every
 * second call to time out.
 */
public class TestTTLExceptionHandingComputeEvents {
	
	private interface Handler {
		
		boolean slowMethod();
		
		Future<Boolean> slowMethodReturnsFuture();
		
		boolean slowMethodThatThrowsException() throws Exception;
		
		boolean slowMethodThatThrowsTineoutException() throws TimeoutException;
	}
	
	private static final class SlowService implements Handler {

		@Override
		public boolean slowMethod() {
			return beSlow();
		}
		
		@Override
		public Future<Boolean> slowMethodReturnsFuture() {
			beSlow();
			return CompletableFuture.completedFuture(true);
		}
		
		@Override
		public boolean slowMethodThatThrowsException() throws Exception {
			return beSlow();
		}
		
		@Override
		public boolean slowMethodThatThrowsTineoutException() throws TimeoutException {
			return beSlow();
		}

		private static boolean beSlow() {
			try {
				Thread.sleep(20);
				return true;
			} catch (InterruptedException e) {
				return false;
			}
		}
	}
	
	private static final class TestTTLExceptionHandingComputeEventsModule extends EventModule {

		@Override
		protected void declare() {
			handle(Handler.class);
			construct(SlowService.class);
			injectingInto(EventProcessor.class)
				.bind(ExecutorService.class).to(() -> Executors.newSingleThreadExecutor());
			bind(EventReflector.class).to(event -> EventProperties.DEFAULT.withTTL(5));
		}
	}
	
	private final Injector injector = Bootstrap.injector(TestTTLExceptionHandingComputeEventsModule.class);
	private final Handler handler = injector.resolve(Handler.class);
	private final SlowService service = injector.resolve(SlowService.class);
	
	@Test
	public void thatTimeoutExceptionIsThrownIfHandlerMethodThrowsSuperclassException() {
		assertThrowsTimeoutException(() ->  handler.slowMethodThatThrowsException());
	}
	
	//TODO flaky test
	@Test
	public void thatTimeoutExceptionIsThrownIfHandlerMethodThrowsTimeoutException() {
		assertThrowsTimeoutException(() -> handler.slowMethodThatThrowsTineoutException());
	}
	
	@Test
	public void thatTimeoutExceptionIsThrownIfFutureGetWithTimeoutIsUsed() {
		assertThrowsTimeoutException(() -> handler.slowMethodReturnsFuture().get(5, TimeUnit.MICROSECONDS));
	}
	
	@Test
	public void thatEventExceptionCausedByTimeoutIsThrownIfFutureGet() {
		assertThrowsEventExceptionCausedByTimeout(() -> handler.slowMethodReturnsFuture().get());
	}
	
	@Test
	public void thatEventExceptionCausedByTimeoutIsThrownIfHandlerMethodNotThrowsException() {
		assertThrowsEventExceptionCausedByTimeout(() -> handler.slowMethod());
	}
	
	private void assertThrowsEventExceptionCausedByTimeout(Callable<Boolean> f) {
		handler.slowMethodReturnsFuture(); // blocks the single thread for 20ms
		try {
			assertFalse("should throw exception", f.call());
		} catch (EventException e) {
			assertSame(TimeoutException.class, e.getCause().getClass());
		} catch (Exception e) {
			fail("should be EventException");
		}
	}
	
	private void assertThrowsTimeoutException(Callable<Boolean> f) {
		assertNotNull(service);
		handler.slowMethodReturnsFuture(); // blocks the single thread for 20ms
		try {
			assertFalse("should throw exception", f.call());
		} catch (Exception e) {
			assertSame(TimeoutException.class, e.getClass());
		}
	}
}
