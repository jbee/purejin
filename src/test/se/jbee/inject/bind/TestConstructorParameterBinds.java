package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.bootstrap.BoundParameter.asType;
import static se.jbee.inject.bootstrap.BoundParameter.constant;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Parameter;
import se.jbee.inject.Supplier;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BoundParameter;
import se.jbee.inject.container.Scoped;

/**
 * The test illustrates how to use {@link Parameter}s to give hints which resources should be
 * injected as constructor arguments.
 * 
 * Thereby it is important to notice that the list of {@linkplain Parameter}s does *not* describe
 * which {@link Constructor} to use! Hence the sequence does *not* has to match the parameter
 * sequence in the constructor definition. As long as types are not assignable a
 * {@linkplain Parameter} is tried to used as the next constructor parameter. The first assignable
 * is used.
 * 
 * @see TestDependencyParameterBinds
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 */
public class TestConstructorParameterBinds {

	private static class Foo {

		final Integer baz;

		@SuppressWarnings ( "unused" )
		Foo( String bar, Integer baz ) {
			this.baz = baz;
			// no use
		}
	}

	private static class Bar {

		final String foo;

		@SuppressWarnings ( "unused" )
		Bar( String foo, Integer baz ) {
			this.foo = foo;
		}
	}

	private static class Baz {

		final String foo;
		final String bar;

		@SuppressWarnings ( "unused" )
		Baz( String foo, String bar ) {
			this.foo = foo;
			this.bar = bar;

		}
	}

	private static class Qux {

		final Serializable value;
		final CharSequence sequence;

		@SuppressWarnings ( "unused" )
		Qux( Serializable value, CharSequence sequence ) {
			this.value = value;
			this.sequence = sequence;
		}
	}

	private static class IncrementingSupplier
			implements Supplier<Integer> {

		int c = 0;

		IncrementingSupplier() {
			// make visible
		}

		@Override
		public Integer supply( Dependency<? super Integer> dependency, Injector injector ) {
			return c++;
		}

	}

	private static class ParameterConstructorBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			Instance<String> y = instance( named( "y" ), raw( String.class ) );
			bind( String.class ).to( "should not be resolved" );
			bind( named( "x" ), String.class ).to( "x" );
			bind( y ).to( "y" );
			bind( Integer.class ).to( 42 );
			bind( Foo.class ).toConstructor( raw( String.class ) );
			bind( Bar.class ).toConstructor( raw( Integer.class ), y );
			bind( Baz.class ).toConstructor( y, y );
			bind( CharSequence.class ).to( String.class ); // should not be used
			bind( Serializable.class ).to( Integer.class ); // should not be used
			bind( Qux.class ).toConstructor( asType( CharSequence.class, y ),
					constant( Number.class, 1980 ) );
			per( Scoped.INJECTION ).bind( named( "inc" ), Foo.class ).toConstructor(
					BoundParameter.supplier( raw( Integer.class ), new IncrementingSupplier() ) );
		}
	}

	private static class FaultyParameterConstructorBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( Bar.class ).toConstructor( raw( Float.class ) );
		}

	}

	private final Injector injector = Bootstrap.injector( ParameterConstructorBindsModule.class );

	@Test
	public void thatClassParameterIsArranged() {
		assertNotNull( injector.resolve( dependency( Foo.class ) ) );
	}

	@Test
	public void thatTypeParameterIsArranged() {
		assertNotNull( injector.resolve( dependency( Bar.class ) ) );
	}

	@Test
	public void thatInstanceParameterIsArranged() {
		Bar bar = injector.resolve( dependency( Bar.class ) );
		assertEquals( "y", bar.foo );
	}

	/**
	 * We can see that {@link BoundParameter#asType(Class, Parameter)} works because the instance y
	 * would also been assignable to the 1st parameter of type {@link Serializable} (in {@link Qux})
	 * but since we typed it as a {@link CharSequence} it no longer is assignable to 1st argument
	 * and will be used as 2nd argument where the as well {@link Serializable} {@link Number}
	 * constant used as 2nd {@link Parameter} is used for as the 1st argument.
	 */
	@Test
	public void thatParameterAsAnotherTypeIsArranged() {
		Qux qux = injector.resolve( dependency( Qux.class ) );
		assertEquals( "y", qux.sequence );
	}

	/**
	 * @see #thatParameterAsAnotherTypeIsArranged()
	 */
	@Test
	public void thatConstantParameterIsArranged() {
		Qux qux = injector.resolve( dependency( Qux.class ) );
		assertEquals( 1980, qux.value );
	}

	@Test
	public void thatReoccuringTypesAreArrangedAsOccuringAfterAnother() {
		Baz baz = injector.resolve( dependency( Baz.class ) );
		assertEquals( "y" , baz.foo );
		assertEquals( "when x alignment after another is broken", "y", baz.bar );
	}

	@Test ( expected = IllegalArgumentException.class )
	public void thatParametersNotArrangedThrowsException() {
		Bootstrap.injector( FaultyParameterConstructorBindsModule.class );
	}

	/**
	 * As an example for a custom {@link Supplier} used as {@link Parameter} a special {@link Foo}
	 * instance "inc" is parameterized with such a parameter. Therefore the passed {@link Integer}
	 * value increments with every injection.
	 */
	@Test
	public void thatCustomParameterIsArranged() {
		Dependency<Foo> incFooDependency = dependency( Foo.class ).named( "inc" );
		Foo foo = injector.resolve( incFooDependency );
		assertEquals( 0, foo.baz.intValue() );

		foo = injector.resolve( incFooDependency );
		assertEquals( 1, foo.baz.intValue() );
	}
}
