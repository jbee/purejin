package se.jbee.inject.bind;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.bootstrap.BindingType.CONSTRUCTOR;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.junit.Test;

import se.jbee.inject.DeclarationType;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Injectron;
import se.jbee.inject.Instance;
import se.jbee.inject.Resource;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.bootstrap.Binding;
import se.jbee.inject.bootstrap.BindingType;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BoundConstructor;
import se.jbee.inject.bootstrap.Bundle;
import se.jbee.inject.bootstrap.Inspect;
import se.jbee.inject.bootstrap.Macro;
import se.jbee.inject.bootstrap.Macros;
import se.jbee.inject.bootstrap.Supply;
import se.jbee.inject.config.Globals;

/**
 * Demonstrates how to use {@link Macro}s to customize the and binding
 * automatics.
 * 
 * In particular the {@link InitialisationMacro} shows how one could initialize
 * fields when an instance is created by using a custom macro that decorates the
 * original {@link Supplier}.
 * 
 * The {@link RequiredConstructorParametersMacro} shows how all parameters of a
 * type bound to a constructor can add bindings that make the parameter's types
 * required so that eager exception occurs if no type is known for a parameter.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestMacroBinds {

	@Target ( { METHOD, FIELD } )
	@Retention ( RUNTIME )
	private static @interface Initialisation {

	}

	private static class Foo {

		@SuppressWarnings ( "unused" )
		Foo( Integer i, Float f ) {
			// no further useage
		}
	}

	private static class Bar {

		@Initialisation
		String s;
	}

	private static class MacroBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( String.class ).to( "answer" );
			bind( Integer.class ).to( 42 );
			bind( Boolean.class ).to( true );
			bind( Foo.class ).toConstructor();
			bind( Number.class ).to( Integer.class );
			bind( Bar.class ).toConstructor();
		}
	}

	private static final class CountMacro
			implements Macro<Binding<?>> {

		int expands = 0;

		CountMacro() {
			// make visible
		}

		@Override
		public <T> void expand(Binding<?> value, Binding<T> incomplete, Bindings bindings) {
			expands++;
		}

	}

	@Test
	public void thatBindingsCanJustBeCounted() {
		CountMacro count = new CountMacro();
		Injector injector = injectorWithMacro( MacroBindsModule.class, count );
		assertEquals( 6, count.expands );
		assertEquals( 0, injector.resolve( Dependency.dependency( Injectron[].class ) ).length );
	}

	private static Injector injectorWithMacro( Class<? extends Bundle> root, Macro<?> macro ) {
		return Bootstrap.injector( root,
				Bindings.bindings( Macros.DEFAULT.with( macro ), Inspect.DEFAULT ), Globals.STANDARD );
	}

	/**
	 * A {@link Macro} that add further bindings to make all types of used
	 * {@link Constructor} parameters {@link DeclarationType#REQUIRED}.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	static final class RequiredConstructorParametersMacro
			implements Macro<BoundConstructor<?>> {

		@Override
		public <T> void expand(BoundConstructor<?> value, Binding<T> incomplete, Bindings bindings) {
			Macros.CONSTRUCTOR.expand( value, incomplete, bindings );
			Type<?>[] params = Type.parameterTypes( value.constructor );
			for ( int i = 0; i < params.length; i++ ) {
				bindings.macros.expandInto(bindings, required( params[i], incomplete ));
			}
		}

		private static <T> Binding<T> required( Type<T> type, Binding<?> binding ) {
			return Binding.binding( new Resource<>( Instance.anyOf( type ) ),
					BindingType.REQUIRED, Supply.<T>required(), binding.scope,
					binding.source.typed( DeclarationType.REQUIRED ) );
		}
	}

	@Test ( expected = NoResourceForDependency.class )
	public void thatAllConstructorParameterTypesCanBeMadeRequired() {
		Macro<?> required = new RequiredConstructorParametersMacro();
		Injector injector = injectorWithMacro( MacroBindsModule.class, required );
		assertNull("we should not get here", injector );
	}

	/**
	 * A simple example-wise {@link Supplier} that allows to initialized newly created instances.
	 * 
	 * In this example a very basic field injection is build but it could be any kind of context
	 * dependent instance initialization.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	private static final class InitialisationSupplier<T>
			implements Supplier<T> {

		private final Supplier<T> decorated;

		InitialisationSupplier( Supplier<T> decorated ) {
			super();
			this.decorated = decorated;
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			T instance = decorated.supply( dependency, injector );
			for ( Field f : instance.getClass().getDeclaredFields() ) {
				if ( f.isAnnotationPresent( Initialisation.class ) ) {
					try {
						f.set( instance, injector.resolve( dependency( Type.fieldType( f ) ) ) );
					} catch ( Exception e ) {
						throw new RuntimeException( e );
					}
				}
			}
			return instance;
		}
	}

	/**
	 * Decorates the usual constructor with initialization.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	static final class InitialisationMacro
			implements Macro<BoundConstructor<?>> {

		@Override
		public <T> void expand(BoundConstructor<?> constructor, Binding<T> incomplete, Bindings bindings) {
			Supplier<T> supplier = new InitialisationSupplier<>(
					Supply.costructor( constructor.typed( incomplete.type() ) ) );
			bindings.macros.expandInto(bindings, incomplete.complete( CONSTRUCTOR, supplier ));
		}

	}

	@Test
	public void thatCustomInitialisationCanBeAdded() {
		Injector injector = injectorWithMacro( MacroBindsModule.class, new InitialisationMacro() );
		assertEquals( "answer", injector.resolve( dependency( Bar.class ) ).s );
	}
}
