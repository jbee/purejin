package test.integration.contract;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.contract.ContractModule;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class TestExceptionHandlingComputeEvents {

	public interface Handler {

		boolean throwsTimeoutException() throws TimeoutException;

		boolean throwsTimeoutException2() throws Exception;

		boolean throwsCheckedException() throws Exception;

		boolean throwsUncheckedException();
	}

	public static final class Service implements Handler {

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

	private static final class TestExceptionHandlingComputeEventsModule
			extends ContractModule {

		@Override
		protected void declare() {
			handle(Handler.class);
			construct(Service.class);
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestExceptionHandlingComputeEventsModule.class);
	private final Service service = injector.resolve(Service.class);
	private final Handler handler = injector.resolve(Handler.class);

	@Test
	void thatUncheckedExceptionIsKept() {
		assertNotNull(service);
		assertException(IllegalArgumentException.class, "original",
				handler::throwsUncheckedException);
	}

	@Test
	void thatCheckedExceptionIsKeptIfHandlerMethodSignatureAllowsIt() {
		assertException(IOException.class, "original",
				handler::throwsCheckedException);
	}

	@Test
	void thatTimeoutIsKeptIfHandlerMethodSignatureAllowsIt() {
		assertException(TimeoutException.class, "original",
				handler::throwsTimeoutException);
	}

	@Test
	void thatTimeoutIsKeptIfHandlerMethodSignatureAllowsItMoreGeneralException() {
		assertException(TimeoutException.class, "original",
				handler::throwsTimeoutException2);
	}

	private static void assertException(Class<? extends Exception> type,
			String msg, Callable<?> test) {
		try {
			test.call();
		} catch (Exception e) {
			assertSame(type, e.getClass());
			assertEquals(msg, e.getMessage());
		}
	}

}
