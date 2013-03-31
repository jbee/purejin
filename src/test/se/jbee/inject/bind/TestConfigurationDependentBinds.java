package se.jbee.inject.bind;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.bind.Configured.configured;
import static se.jbee.inject.bootstrap.Inspect.methodsReturn;
import static se.jbee.inject.util.Typecast.providerTypeOf;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.bind.TestInspectorBinds.Resource;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.util.Provider;
import se.jbee.inject.util.Scoped;

/**
 * This test demonstrates how to switch between different implementations during runtime dependent
 * on a setting in some configuration object.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestConfigurationDependentBinds {

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

	private static class Configuration {

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
	 * This module demonstrates CDI on a low level using general binds.
	 */
	private static class ConfigurationDependentBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			per( Scoped.INJECTION ).bind( Validator.class ).to(
					configured( anyOf( ValidationStrength.class ) ) );
			bind( named( (ValidationStrength) null ), Validator.class ).to( Permissive.class );
			bind( named( ValidationStrength.PERMISSIVE ), Validator.class ).to( Permissive.class );
			bind( named( ValidationStrength.STRICT ), Validator.class ).to( Strict.class );

			// the below is just *a* example - it is just important to provide the 'value' per injection
			per( Scoped.INJECTION ).bind( methodsReturn( raw( ValidationStrength.class ) ) ).in(
					Configuration.class );
		}
	}

	/**
	 * The same as above using {@link #configbind(Class)}s. The important difference is that it is
	 * not required to manually bind to a {@link Configured} value. This is done implicitly with
	 * each of the configbind calls. This also allow to use them in different modules without
	 * causing clashes.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	private static class ConfigurationDependentBindsModule2
			extends BinderModule {

		@Override
		protected void declare() {
			configbind( Validator.class ).on( ValidationStrength.PERMISSIVE ).to( Permissive.class );
			configbind( Validator.class ).on( ValidationStrength.STRICT ).to( Strict.class );
			configbind( Validator.class ).onOther( ValidationStrength.class ).to( Permissive.class );

			// the below is just *a* example - it is just important to provide the 'value' per injection
			per( Scoped.INJECTION ).bind( methodsReturn( raw( ValidationStrength.class ) ) ).in(
					Configuration.class );
		}

	}

	private static class ConfigurationDependentBindsModule3
			extends BinderModule {

		@Override
		protected void declare() {
			Name answer = named( "answer" );
			ConfigBinder<String> bind = configbind( String.class );
			bind.on( answer, 42 ).to( "Now it is 42" );
			bind.on( answer, 7 ).to( "Now it is 7" );
			bind.onOther( answer, Integer.class ).to( "Default is undefined" );

			// the below is just *a* example - it is just important to provide the 'value' per injection
			per( Scoped.INJECTION ).bind(
					methodsReturn( raw( int.class ) ).namedBy( Resource.class ) ).in(
					Configuration.class );
		}

	}

	private static class ConfigurationDependentBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( ConfigurationDependentBindsModule.class );
			install( BuildinBundle.PROVIDER );
		}

	}

	@Test
	public void thatReconfigurationIsResolvedToAnotherImplementation() {
		Injector injector = Bootstrap.injector( ConfigurationDependentBindsModule.class );
		assertReconfigurationIsResolvedToAnotherImplementation( injector );
	}

	@Test
	public void thatReconfigurationIsResolvedToAnotherImplementation2() {
		Injector injector = Bootstrap.injector( ConfigurationDependentBindsModule2.class );
		assertReconfigurationIsResolvedToAnotherImplementation( injector );
	}

	private static void assertReconfigurationIsResolvedToAnotherImplementation( Injector injector ) {
		Dependency<Validator> dependency = dependency( Validator.class );
		Configuration config = injector.resolve( dependency( Configuration.class ) );
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
	public void thatReconfigurationIsProvidedToAnotherImplementation() {
		Injector injector = Bootstrap.injector( ConfigurationDependentBindsBundle.class );
		Configuration config = injector.resolve( dependency( Configuration.class ) );
		Provider<Validator> v = injector.resolve( dependency( providerTypeOf( Validator.class ) ) );
		String input = "input";
		assertTrue( v.provide().valid( input ) );
		config.setValidationStrength( ValidationStrength.STRICT );
		assertFalse( v.provide().valid( input ) );
		config.setValidationStrength( ValidationStrength.PERMISSIVE );
		assertTrue( v.provide().valid( input ) );
	}

	@Test
	public void thatReconfigurationResolvedUsingNamedInstances() {
		assertConfigNumberResolvedToStringEnding( null, "undefined" );
		assertConfigNumberResolvedToStringEnding( 7, "7" );
		assertConfigNumberResolvedToStringEnding( 42, "42" );
		assertConfigNumberResolvedToStringEnding( 123, "undefined" ); // equal to null
	}

	private static void assertConfigNumberResolvedToStringEnding( Integer configValue, String ending ) {
		Injector injector = Bootstrap.injector( ConfigurationDependentBindsModule3.class );
		Configuration config = injector.resolve( dependency( Configuration.class ) );
		config.setNumber( configValue );
		String v = injector.resolve( dependency( String.class ) );
		assertTrue( v.endsWith( ending ) );
	}
}
