package se.jbee.inject.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;

public class TestExceptionHandlingComputeEvents {

	private interface Handler {
		
		boolean throwsTimeoutException() throws TimeoutException;

		boolean throwsTimeoutException2() throws Exception;
		
		boolean throwsCheckedException() throws Exception;
		
		boolean throwsUncheckedException();
	}
	
	private static final class Service implements Handler {

		@Override
		public boolean throwsTimeoutException() throws TimeoutException {
			throw new TimeoutException("original");
		}
		
		@Override
		public boolean throwsTimeoutException2() throws TimeoutException {
			throw new TimeoutException("original");
		}

		@Override
		public boolean throwsCheckedException() throws Exception {
			throw new IOException("original");
		}

		@Override
		public boolean throwsUncheckedException() {
			throw new IllegalArgumentException("original");
		}
		
	}
	
	private static final class TestExceptionHandlingComputeEventsModule extends EventModule {

		@Override
		protected void declare() {
			handle(Handler.class);
			construct(Service.class);
		}
	}
	
	private final Injector injector = Bootstrap.injector(TestExceptionHandlingComputeEventsModule.class);
	private final Service service = injector.resolve(Service.class);
	private final Handler handler = injector.resolve(Handler.class);

	@Test
	public void thatUncheckedExceptionIsKept() {
		assertNotNull(service);
		assertException(IllegalArgumentException.class, "original", () -> handler.throwsUncheckedException());
	}

	@Test
	public void thatCheckedExceptionIsKeptIfHandlerMethodSignatureAllowsIt() {
		assertException(IOException.class, "original", () -> handler.throwsCheckedException());
	}
	
	@Test
	public void thatTimeoutIsKeptIfHandlerMethodSignatureAllowsIt() {
		assertException(TimeoutException.class, "original", () -> handler.throwsTimeoutException());
	}
	
	@Test
	public void thatTimeoutIsKeptIfHandlerMethodSignatureAllowsItMoreGeneralException() {
		assertException(TimeoutException.class, "original", () -> handler.throwsTimeoutException2());
	}
	
	private static void assertException(Class<? extends Exception> type, String msg, Callable<?> test) {
		try {
			test.call();
		} catch (Exception e) {
			assertSame(type, e.getClass());
			assertEquals(msg, e.getMessage());
		}
	}
	
}
