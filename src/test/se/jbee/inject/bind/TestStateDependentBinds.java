package se.jbee.inject.bind;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.bootstrap.Inspect.methodsReturn;
import static se.jbee.inject.container.Typecast.providerTypeOf;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.bind.TestInspectorBinds.Resource;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.bootstrap.Supply;
import se.jbee.inject.container.Provider;
import se.jbee.inject.container.Scoped;

/**
 * This test demonstrates how to switch between different implementations during runtime dependent
 * on a setting in some setting object.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestStateDependentBinds {

	private static interface Validator {

		boolean valid( String input );
	}

	private static enum ValidationStrength {
		PERMISSIVE,
		STRICT
	}

	private static class Permissive
			implements Validator {

		@Override
		public boolean valid( String input ) {
			return true; // just for testing
		}

	}

	private static class Strict
			implements Validator {

		@Override
		public boolean valid( String input ) {
			return false; // just for testing
		}

	}

	private static class StatefulObject {

		private ValidationStrength validationStrength;
		private Integer number;

		/**
		 * Will be detected as service method and thereby used
		 */
		@SuppressWarnings ( "unused" )
		public ValidationStrength getValidationStrength() {
			return validationStrength;
		}

		public void setValidationStrength( ValidationStrength validationStrength ) {
			this.validationStrength = validationStrength;
		}

		/**
		 * Will be detected as service method and thereby used
		 */
		@Resource ( "answer" )
		public Integer getNumber() {
			return number;
		}

		public void setNumber( Integer number ) {
			this.number = number;
		}

	}

	/*
	 * Module and Bundle code to setup scenario
	 */

	/**
	 * This module demonstrates state dependent binds on a low level using general binds.
	 */
	private static class StateDependentBindsModule1
			extends BinderModule {

		@Override
		protected void declare() {
			per( Scoped.INJECTION ).bind( Validator.class ).to(	Supply.stateDependent(Type.raw(Validator.class), dependency(ValidationStrength.class)) );
			
			bind( named( ValidationStrength.PERMISSIVE ), Validator.class ).to(	Permissive.class );
			bind( named( ValidationStrength.STRICT ), Validator.class ).to( Strict.class );
			bind( named( (Object)null ), Validator.class ).to( Permissive.class );

			// the below is just *a* example - it is just important to provide the 'value' per injection
			per( Scoped.INJECTION ).bind( methodsReturn( raw( ValidationStrength.class ) ) ).in( StatefulObject.class );
		}
	}

	/**
	 * The same as above using {@link #connect(Class)}s. The important difference is that it is
	 * not required to manually bind to a {@link State} value. This is done implicitly with
	 * each of the configbind calls. This also allow to use them in different modules without
	 * causing clashes.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	private static class StateDependentBindsModule2
			extends BinderModule {

		@Override
		protected void declare() {
			connect(Validator.class).via(ValidationStrength.class);

			bind( named( ValidationStrength.PERMISSIVE ), Validator.class ).to(	Permissive.class );
			bind( named( ValidationStrength.STRICT ), Validator.class ).to( Strict.class );
			bind( named( (Object)null ), Validator.class ).to( Permissive.class );

			// the below is just *a* example - it is just important to provide the 'value' per injection
			per( Scoped.INJECTION ).bind( methodsReturn( raw( ValidationStrength.class ) ) ).in( StatefulObject.class );
		}

	}

	private static class StateDependentBindsModule3
			extends BinderModule {

		@Override
		protected void declare() {
			connect(String.class).via(Integer.class);
			
			bind(named(42), String.class).to( "Now it is 42" );
			bind(named(7), String.class).to( "Now it is 7" );
			bind(named((Object)null), String.class).to( "Default is undefined" );

			// the below is just *a* example - it is just important to provide the 'value' per injection
			per( Scoped.INJECTION ).bind( methodsReturn( raw( int.class ) ).namedBy( Resource.class ) ).in(	StatefulObject.class );
		}

	}

	private static class StateDependentBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( StateDependentBindsModule1.class );
			install( BuildinBundle.PROVIDER );
		}

	}

	@Test
	public void thatStateChangeIsResolvedToAnotherImplementation() {
		Injector injector = Bootstrap.injector( StateDependentBindsModule1.class );
		assertStateChangeIsResolvedToAnotherImplementation( injector );
	}

	@Test
	public void thatStateChangeIsResolvedToAnotherImplementation2() {
		Injector injector = Bootstrap.injector( StateDependentBindsModule2.class );
		assertStateChangeIsResolvedToAnotherImplementation( injector );
	}

	private static void assertStateChangeIsResolvedToAnotherImplementation( Injector injector ) {
		Dependency<Validator> dependency = dependency( Validator.class );
		StatefulObject config = injector.resolve( dependency( StatefulObject.class ) );
		Validator v = injector.resolve( dependency );
		String input = "input";
		assertTrue( v.valid( input ) ); // default was PERMISSIVE
		config.setValidationStrength( ValidationStrength.STRICT );
		v = injector.resolve( dependency );
		assertFalse( v.valid( input ) ); // STRICT
		config.setValidationStrength( ValidationStrength.PERMISSIVE );
		v = injector.resolve( dependency );
		assertTrue( v.valid( input ) ); // PERMISSIVE
	}

	@Test
	public void thatStateChangeIsProvidedToAnotherImplementation() {
		Injector injector = Bootstrap.injector( StateDependentBindsBundle.class );
		StatefulObject config = injector.resolve( dependency( StatefulObject.class ) );
		Provider<Validator> v = injector.resolve( dependency( providerTypeOf( Validator.class ) ) );
		String input = "input";
		assertTrue( v.provide().valid( input ) );
		config.setValidationStrength( ValidationStrength.STRICT );
		assertFalse( v.provide().valid( input ) );
		config.setValidationStrength( ValidationStrength.PERMISSIVE );
		assertTrue( v.provide().valid( input ) );
	}

	@Test
	public void thatStateChangeIsResolvedUsingNamedInstances() {
		assertConfigNumberResolvedToStringEnding( null, "undefined" );
		assertConfigNumberResolvedToStringEnding( 7, "7" );
		assertConfigNumberResolvedToStringEnding( 42, "42" );
		assertConfigNumberResolvedToStringEnding( 123, "undefined" ); // equal to null
	}

	private static void assertConfigNumberResolvedToStringEnding( Integer actualValue, String ending ) {
		Injector injector = Bootstrap.injector( StateDependentBindsModule3.class );
		StatefulObject state = injector.resolve( dependency( StatefulObject.class ) );
		state.setNumber( actualValue );
		String v = injector.resolve( dependency( String.class ) );
		assertTrue( v.endsWith( ending ) );
	}
}
