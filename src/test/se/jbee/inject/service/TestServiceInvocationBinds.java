package se.jbee.inject.service;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Type.raw;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.bind.Bootstrap;
import se.jbee.inject.bind.BootstrapperBundle;
import se.jbee.inject.service.ServiceInvocation;
import se.jbee.inject.service.ServiceMethod;
import se.jbee.inject.service.ServiceModule;
import se.jbee.inject.service.ServiceInvocation.ServiceInvocationExtension;
import se.jbee.inject.util.Value;

public class TestServiceInvocationBinds {

	private static class ServiceInvocationBindsModule
			extends ServiceModule {

		@Override
		protected void declare() {
			bindServiceMethodsIn( ServiceInvocationBindsService.class );
			extend( ServiceInvocationExtension.RETURN_TYPE, AssertInvocation.class );
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
		public <P, R> void after( Value<P> parameter, Value<R> result, Long before ) {
			afterCount++;
			assertEquals( "before state passed to after", 42L, before.longValue() );
			assertEquals( result.getValue(), "Foo".hashCode() );
		}

		@Override
		public <P, R> Long before( Value<P> parameter, Type<R> result ) {
			beforeCount++;
			assertTrue( "right type passed to before", parameter.getType().equalTo(
					raw( String.class ) ) );
			assertEquals( "right value passed to before", parameter.getValue(), "Foo" );
			return 42L;
		}

		@Override
		public <P, R> void afterException( Value<P> parameter, Type<R> result, Exception e,
				Long before ) {
			afterExceptionCount++;
			assertTrue( e instanceof IllegalStateException );
		}

	}

	private final Injector injector = Bootstrap.injector( ServiceInvocationBindsBundle.class );
	private final AssertInvocation inv = injector.resolve( dependency( AssertInvocation.class ) );

	@Test
	@SuppressWarnings ( "unchecked" )
	public void thatInvocationIsInvokedBeforeAndAfterTheServiceMethodCall() {
		Dependency<ServiceMethod> dependency = dependency( raw( ServiceMethod.class ).parametized(
				String.class, Integer.class ) );
		ServiceMethod<String, Integer> hashCode = injector.resolve( dependency );
		int beforeCount = inv.beforeCount;
		int afterCount = inv.afterCount;
		assertEquals( "Foo".hashCode(), hashCode.invoke( "Foo" ).intValue() );
		assertEquals( beforeCount + 1, inv.beforeCount );
		assertEquals( afterCount + 1, inv.afterCount );
	}

	@Test
	@SuppressWarnings ( "unchecked" )
	public void thatInvocationIsInvokedAfterExceptionInTheServiceMethodCall() {
		Dependency<ServiceMethod> dependency = dependency( raw( ServiceMethod.class ).parametized(
				String.class, Void.class ) );
		ServiceMethod<String, Void> fail = injector.resolve( dependency );
		int afterExceptionCount = inv.afterExceptionCount;
		try {
			fail.invoke( "Foo" );
		} catch ( RuntimeException e ) {
			assertTrue( e.getCause() instanceof IllegalStateException );
			assertEquals( e.getCause().getMessage(), "Foo" );
			assertEquals( afterExceptionCount + 1, inv.afterExceptionCount );
			return;
		}
		fail( "Exception expected" );
	}
}
