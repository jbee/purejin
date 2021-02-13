package test.integration.contract;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.contract.*;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static test.integration.util.TestUtils.wait20;
import static test.integration.util.TestUtils.wait50;

/**
 * Tests to verify the correctness of the exception handling caused by timeouts
 * related to the {@link EventPolicy#ttl} period.
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
 * {@link Thread#sleep(long)} of 40ms and using a {@link ExecutorService} with a
 * single thread. Thereby a already running event will stop further processing
 * of other events for about 40ms. This gives a window in which further added
 * events (calls) can time out. The TTL is set to just 5ms which causes every
 * second call to time out.
 */
@Disabled
class TestTTLExceptionHandingComputeEvents {

	private interface Handler {

		boolean slowMethod();

		Future<Boolean> slowMethodReturnsFuture();

		boolean slowMethodThatThrowsException() throws Exception;

		boolean slowMethodThatThrowsTineoutException() throws TimeoutException;
	}

	private static final class SlowService implements Handler {

		@Override
		public boolean slowMethod() {
			return wait50();
		}

		@Override
		public Future<Boolean> slowMethodReturnsFuture() {
			wait50();
			return CompletableFuture.completedFuture(true);
		}

		@Override
		public boolean slowMethodThatThrowsException() throws Exception {
			return wait50();
		}

		@Override
		public boolean slowMethodThatThrowsTineoutException()
				throws TimeoutException {
			return wait50();
		}

	}

	private static final class TestTTLExceptionHandingComputeEventsModule
			extends ContractModule {

		@Override
		protected void declare() {
			handle(Handler.class);
			construct(SlowService.class);
			injectingInto(EventProcessor.class).bind(ExecutorService.class).toProvider(
					Executors::newSingleThreadExecutor);
			bind(PolicyProvider.class).to(event -> EventPolicy.DEFAULT.withTTL(5));
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestTTLExceptionHandingComputeEventsModule.class);
	private final Handler handler = injector.resolve(Handler.class);
	private final SlowService service = injector.resolve(SlowService.class);

	@Test
	void thatTimeoutExceptionIsThrownIfHandlerMethodThrowsSuperclassException() {
		assertThrowsTimeoutException(
				handler::slowMethodThatThrowsException);
	}

	@Test
	void thatTimeoutExceptionIsThrownIfHandlerMethodThrowsTimeoutException() {
		assertThrowsTimeoutException(
				handler::slowMethodThatThrowsTineoutException);
	}

	@Test
	void thatTimeoutExceptionIsThrownIfFutureGetWithTimeoutIsUsed() {
		assertThrowsTimeoutException(
				() -> handler.slowMethodReturnsFuture().get(5,
						TimeUnit.MICROSECONDS));
	}

	@Test
	void thatEventExceptionCausedByTimeoutIsThrownIfFutureGet() {
		assertThrowsEventExceptionCausedByTimeout(
				() -> handler.slowMethodReturnsFuture().get());
	}

	@Test
	void thatEventExceptionCausedByTimeoutIsThrownIfHandlerMethodNotThrowsException() {
		assertThrowsEventExceptionCausedByTimeout(handler::slowMethod);
	}

	private void assertThrowsEventExceptionCausedByTimeout(
			Callable<Boolean> f) {
		assertNotNull(service);
		blockProcessorWithTask();
		try {
			assertFalse(f.call(), "should throw EventException ");
		} catch (EventException e) {
			assertSame(TimeoutException.class, e.getCause().getClass());
		} catch (Exception e) {
			fail("should be EventException");
		}
	}

	private void assertThrowsTimeoutException(Callable<Boolean> f) {
		assertNotNull(service);
		blockProcessorWithTask();
		try {
			assertFalse(f.call(), "should throw TimeoutException");
		} catch (Exception e) {
			assertSame(TimeoutException.class, e.getClass());
		}
	}

	private void blockProcessorWithTask() {
		handler.slowMethodReturnsFuture(); // blocks the single thread for 20ms
		wait20(); // make sure the task really gets started before we add the 2nd one
	}
}
