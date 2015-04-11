package se.jbee.inject.bind;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;

import org.junit.Test;

import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * In Silk primitives and wrapper {@link Class}es are the same {@link Type}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 */
public class TestPrimitiveBinds {

	private static class PrimitiveBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( int.class ).to( 42 );
			bind( boolean.class ).to( true );
			bind( long.class ).to( 132L );
			bind( named( "pi" ), float.class ).to( 3.1415f );
			bind( named( "e" ), double.class ).to( 2.71828d );
			bind( PrimitiveBindsBean.class ).toConstructor();
		}

	}

	private static class PrimitiveBindsBean {

		final int i;
		final float f;
		final boolean b;
		final Integer bigI;
		final Float bigF;
		final Boolean bigB;

		@SuppressWarnings ( "unused" )
		PrimitiveBindsBean( int i, float f, boolean b, Integer bigI, Float bigF, Boolean bigB ) {
			super();
			this.i = i;
			this.f = f;
			this.b = b;
			this.bigI = bigI;
			this.bigF = bigF;
			this.bigB = bigB;
		}

	}

	private final Injector injector = Bootstrap.injector( PrimitiveBindsModule.class );

	@Test
	public void thatIntPrimitivesWorkAsWrapperClasses() {
		assertThat( injector.resolve( dependency( Integer.class ) ), is( 42 ) );
		assertThat( injector.resolve( dependency( int.class ) ), is( 42 ) );
	}

	@Test
	public void thatLongPrimitivesWorkAsWrapperClasses() {
		assertThat( injector.resolve( dependency( Long.class ) ), is( 132L ) );
		assertThat( injector.resolve( dependency( long.class ) ), is( 132L ) );
	}

	@Test
	public void thatBooleanPrimitivesWorkAsWrapperClasses() {
		assertThat( injector.resolve( dependency( Boolean.class ) ), is( true ) );
		assertThat( injector.resolve( dependency( boolean.class ) ), is( true ) );
	}

	@Test
	public void thatFloatPrimitivesWorkAsWrapperClasses() {
		assertThat( injector.resolve( dependency( Float.class ).named( "pi" ) ), is( 3.1415f ) );
		assertThat( injector.resolve( dependency( float.class ).named( "pi" ) ), is( 3.1415f ) );
	}

	@Test
	public void thatDoublePrimitivesWorkAsWrapperClasses() {
		assertThat( injector.resolve( dependency( Double.class ).named( "e" ) ), is( 2.71828d ) );
		assertThat( injector.resolve( dependency( double.class ).named( "e" ) ), is( 2.71828d ) );
	}

	/**
	 * To allow such a automatic conversion a bridge could be bound for each of the primitives. Such
	 * a bind would look like this.
	 * 
	 * <pre>
	 * bind( int[].class ).to( new IntToIntergerArrayBridgeSupplier() );
	 * </pre>
	 * 
	 * The stated bridge class is not a part of Silk but easy to do. Still a loot of code for all
	 * the primitives for a little benefit.
	 */
	@Test ( expected = NoResourceForDependency.class )
	public void thatPrimitveArrayNotWorkAsWrapperArrayClasses() {
		assertArrayEquals( new int[42], injector.resolve( dependency( int[].class ) ) );
	}

	@Test
	public void thatPrimitivesWorkAsPrimitiveOrWrapperClassesWhenInjected() {
		PrimitiveBindsBean bean = injector.resolve( dependency( PrimitiveBindsBean.class ) );
		assertThat( bean.i, is( 42 ) );
		assertThat( bean.f, is( 3.1415f ) );
		assertThat( bean.b, is( true ) );
		assertThat( bean.bigI, is( 42 ) );
		assertThat( bean.bigF, is( 3.1415f ) );
		assertThat( bean.bigB, is( true ) );
	}
}
