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
import se.jbee.inject.Instance;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.bind.Binder.RootBinder;
import se.jbee.inject.bind.TestInspectorBinds.Resource;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.container.Provider;
import se.jbee.inject.container.Scoped;

/**
 * This test demonstrates how to switch between different implementations during
 * runtime dependent on a setting in some setting object. This example shows
 * also how to extend the {@link BinderModule} to introduce a custom utility
 * such as {@link ControllerModule#connect(Class)}
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
	
	public static abstract class ControllerModule extends BinderModule {
		
		public <T, C> ControllerBinder<T> connect( Class<T> type ) {
			return connect( raw( type ) );
		}

		public <T, C> ControllerBinder<T> connect( Type<T> type ) {
			return new ControllerBinder<>( root, type );
		}
	}
	
	public static class ControllerBinder<T> {

		private final RootBinder binder;
		private final Type<T> type;

		ControllerBinder( RootBinder binder, Type<T> type ) {
			super();
			this.binder = new RootBinder( binder.bind().next() );
			this.type = type;
		}

		public <S> void via(Class<S> state) {
			via(raw(state));
		}

		public <S> void via(Type<S> state) {
			binder.per(Scoped.INJECTION).bind( type ).to( stateDependent(type, Dependency.dependency(state)) );
		}
		
	}
	
	/**
	 * This is a indirection that resolves a {@link Type} dependent on another current
	 * {@link State} value. This can be understand as a dynamic <i>name</i> switch so that a
	 * call is resolved to different named instances.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	private static final class StateDependentSupplier<T, S>
			implements Supplier<T> {

		private final Type<T> type;
		private final Dependency<S> state;

		StateDependentSupplier( Type<T> type, Dependency<S> state ) {
			super();
			this.type = type;
			this.state = state;
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			final S actualState = injector.resolve( state );
			return supply( dependency, injector, actualState );
		}

		private T supply( Dependency<? super T> dependency, Injector injector, final S actualState ) {
			final Instance<T> actualToInject = Instance.instance( named(actualState), type );
			try {
				return injector.resolve( dependency.instanced( actualToInject ) );
			} catch ( NoResourceForDependency e ) {
				if (actualState != null) { // when not trying default
					return supply( dependency, injector, null ); // try default
				}
				throw e;
			}
		}
	}	

	/**
	 * This module demonstrates state dependent binds on a low level using general binds.
	 */
	private static class StateDependentBindsModule1
			extends BinderModule {

		@Override
		protected void declare() {
			per( Scoped.INJECTION ).bind( Validator.class ).to(	stateDependent(Type.raw(Validator.class), dependency(ValidationStrength.class)) );
			
			bind( named( ValidationStrength.PERMISSIVE ), Validator.class ).to(	Permissive.class );
			bind( named( ValidationStrength.STRICT ), Validator.class ).to( Strict.class );
			bind( named( (Object)null ), Validator.class ).to( Permissive.class );

			// the below is just *a* example - it is just important to provide the 'value' per injection
			per( Scoped.INJECTION ).bind( methodsReturn( raw( ValidationStrength.class ) ) ).in( StatefulObject.class );
		}
	}

	public static <T, C> Supplier<T> stateDependent( Type<T> type, Dependency<C> state ) {
		return new StateDependentSupplier<>( type, state );
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
			extends ControllerModule {

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
			extends ControllerModule {

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
