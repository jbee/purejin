package se.jbee.inject.bind;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static se.jbee.inject.Dependency.dependency;

import org.junit.Test;

import se.jbee.inject.DIRuntimeException.NoSuchResourceException;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;

public class TestRequiredProvidedBinds {

	private static interface ExampleService {
		// a classic singleton bean
	}

	private static class ExampleServiceImpl
			implements ExampleService {
		// and its implementation
	}

	private static class UnusedImpl {
		// just something we provide but do not require 
	}

	private static class RequirementModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( Integer.class ).to( 42 );
			require( ExampleService.class );
		}

	}

	private static class ProvidingModule
			extends BinderModule {

		@Override
		protected void declare() {
			provide( ExampleServiceImpl.class );
			provide( UnusedImpl.class );
		}

	}

	private static class RequiredProvidedBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( RequirementModule.class );
			install( ProvidingModule.class );
		}
	}

	@Test ( expected = IllegalStateException.class )
	public void thatNotProvidedRequiredBindThrowsException() {
		Bootstrap.injector( RequirementModule.class );
	}

	@Test
	public void thatRequirementIsFulfilledByProvidedBind() {
		Injector injector = Bootstrap.injector( RequiredProvidedBindsBundle.class );
		assertNotNull( injector.resolve( dependency( ExampleService.class ) ) );
	}

	@Test
	public void thatUnusedProvidedBindIsNotAddedToInjectorContext() {
		Injector injector = Bootstrap.injector( RequiredProvidedBindsBundle.class );
		try {
			injector.resolve( dependency( UnusedImpl.class ) );
			fail( "Should not be bound and therefore throw below exception" );
		} catch ( NoSuchResourceException e ) {
			// expected this
		} catch ( Throwable e ) {
			fail( "Expected another exception but got: " + e );
		}
	}
}
