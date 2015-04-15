package se.jbee.inject.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Type.raw;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;

public class TestServiceInvocationBinds {

	private static class ServiceInvocationBindsModule
			extends ServiceModule {

		@Override
		protected void declare() {
			bindServiceMethodsIn( ServiceInvocationBindsService.class );
			bindInvocationHandler(AssertInvocation.class );
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
		Dependency<ServiceMethod> dependency = dependency( raw( ServiceMethod.class ).parametized(
				String.class, Integer.class ) );
		ServiceMethod<String, Integer> hashCode = injector.resolve( dependency );
		int beforeCount = inv.beforeCount;
		int afterCount = inv.afterCount;
		assertEquals( "Foo".hashCode(), hashCode.invoke( "Foo" ).intValue() );
		assertEquals( beforeCount + 1, inv.beforeCount );
		assertEquals( afterCount + 1, inv.afterCount );
	}

	@SuppressWarnings ( "unchecked" )
	@Test
	public void thatInvocationIsInvokedAfterExceptionInTheServiceMethodCall() {
		@SuppressWarnings ( "rawtypes" )
		Dependency<ServiceMethod> dependency = dependency( raw( ServiceMethod.class ).parametized(
				String.class, Void.class ) );
		ServiceMethod<String, Void> fail = injector.resolve( dependency );
		int afterExceptionCount = inv.afterExceptionCount;
		try {
			fail.invoke( "Foo" );
		} catch ( ServiceMalfunction e ) {
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
}
