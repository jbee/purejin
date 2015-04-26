package se.jbee.inject.procedure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Type.raw;

import java.lang.reflect.Method;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency.SupplyFailed;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.bootstrap.Invoke;

public class TestServiceInvocationBinds {

	private static class ServiceInvocationBindsModule
	extends ProcedureModule {

		@Override
		protected void declare() {
			bindProceduresIn( ServiceInvocationBindsService.class );
			plug(AssertInvocation.class).into(ServiceInvocation.class);
			bind(Executor.class).to(InstrumentedExecutor.class);
		}

	}

	private static class ServiceInvocationBindsBundle
	extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( ServiceInvocationBindsModule.class );
		}

	}

	@SuppressWarnings ( "unused" )
	private static class ServiceInvocationBindsService {

		public int hashCode( String s ) {
			return s.hashCode();
		}

		public void fail( String text ) {
			throw new IllegalStateException( text );
		}
	}

	private static class AssertInvocation
	implements ServiceInvocation<Long> {

		int afterCount;
		int beforeCount;
		int afterExceptionCount;

		@Override
		public <P, R> void after(Type<P> parameter, P value, Type<R> resultType, R result, Long before) {
			afterCount++;
			assertEquals( "before state passed to after", 42L, before.longValue() );
			assertEquals( result, "Foo".hashCode() );
		}

		@Override
		public <P, R> Long before(Type<P> parameter, P value, Type<R> result) {
			beforeCount++;
			assertTrue( "right type passed to before",
					parameter.type().equalTo( raw( String.class ) ) );
			assertEquals( "right value passed to before", value, "Foo" );
			return 42L;
		}

		@Override
		public <P, R> void afterException(Type<P> parameter, P value, Type<R> result, Exception e, Long before) {
			afterExceptionCount++;
			assertTrue( e instanceof IllegalStateException );
		}

	}

	private final Injector injector = Bootstrap.injector( ServiceInvocationBindsBundle.class );
	private final AssertInvocation inv = injector.resolve( dependency( AssertInvocation.class ) );

	@SuppressWarnings ( "unchecked" )
	@Test
	public void thatInvocationIsInvokedBeforeAndAfterTheServiceMethodCall() {
		@SuppressWarnings ( "rawtypes" )
		Dependency<Procedure> dependency = dependency( raw( Procedure.class ).parametized(
				String.class, Integer.class ) );
		Procedure<String, Integer> hashCode = injector.resolve( dependency );
		int beforeCount = inv.beforeCount;
		int afterCount = inv.afterCount;
		assertEquals( "Foo".hashCode(), hashCode.run( "Foo" ).intValue() );
		assertEquals( beforeCount + 1, inv.beforeCount );
		assertEquals( afterCount + 1, inv.afterCount );
	}

	@SuppressWarnings ( "unchecked" )
	@Test
	public void thatInvocationIsInvokedAfterExceptionInTheServiceMethodCall() {
		@SuppressWarnings ( "rawtypes" )
		Dependency<Procedure> dependency = dependency( raw( Procedure.class ).parametized(
				String.class, Void.class ) );
		Procedure<String, Void> fail = injector.resolve( dependency );
		int afterExceptionCount = inv.afterExceptionCount;
		try {
			fail.run( "Foo" );
		} catch ( ProcedureMalfunction e ) {
			assertTrue( e.getCause() instanceof IllegalStateException );
			assertEquals( e.getCause().getMessage(), "Foo" );
			assertEquals( afterExceptionCount + 1, inv.afterExceptionCount );
			return;
		}
		fail( "Exception expected" );
	}

	@Test
	public void thatInvocationHandlersCanBeResolvedDirectly() {
		ServiceInvocation<?>[] invocations = injector.resolve(dependency(ServiceInvocation[].class));
		assertEquals(1, invocations.length);
		assertEquals(invocations[0].getClass(), AssertInvocation.class);
	}

	static final class InstrumentedExecutor implements Executor {

		private final ServiceInvocation<?>[] invocations;

		InstrumentedExecutor( Injector injector ) { //TODO why doesn't it work to ask for the array of ServiceInvocation directly?
			super();
			this.invocations = injector.resolve(dependency(ServiceInvocation[].class));
		}

		@Override
		public <I, O> O run(Object impl, Method proc, Object[] args, Type<O> output, Type<I> input, I value) throws ProcedureMalfunction {
			Object[] state = before( output, input, value );
			O res = null;
			try {
				res = output.rawType.cast(Invoke.method(proc, impl, args));
			} catch ( SupplyFailed e ) {
				Exception ex = e;
				if ( e.getCause() instanceof Exception ) {
					ex = (Exception) e.getCause();
				}
				afterException(output, input, value, ex, state );
				throw new ProcedureMalfunction(proc.getDeclaringClass().getSimpleName()+"#"+proc.getName()+" failed: "+e.getMessage(), ex );
			}
			after( output, input, value, res, state );
			return res;
		}

		@SuppressWarnings ( "unchecked" )
		private <T, I, O> void afterException( Type<O> output, Type<I> input, I value, Exception e, Object[] states ) {
			if ( invocations.length == 0 ) {
				return;
			}
			for ( int i = 0; i < invocations.length; i++ ) {
				try {
					ServiceInvocation<T> inv = (ServiceInvocation<T>) invocations[i];
					inv.afterException( input, value, output, e, (T) states[i] );
				} catch ( RuntimeException re ) {
					// warn that invocation before had thrown an exception
				}
			}
		}

		private <I,O> Object[] before( Type<O> output, Type<I> input, I value ) {
			if ( invocations.length == 0 ) {
				return null;
			}
			Object[] invokeState = new Object[invocations.length];
			for ( int i = 0; i < invocations.length; i++ ) {
				try {
					invokeState[i] = invocations[i].before( input, value, output );
				} catch ( RuntimeException e ) {
					// warn that invocation before had thrown an exception
				}
			}
			return invokeState;
		}

		@SuppressWarnings ( "unchecked" )
		private <T, I, O> void after( Type<O> output, Type<I> input, I value, O res, Object[] states ) {
			if ( invocations.length == 0 ) {
				return;
			}
			for ( int i = 0; i < invocations.length; i++ ) {
				try {
					ServiceInvocation<T> inv = (ServiceInvocation<T>) invocations[i];
					inv.after( input, value, output, res, (T)states[i] );
				} catch ( RuntimeException e ) {
					// warn that invocation before had thrown an exception
				}
			}
		}

	}	
}
