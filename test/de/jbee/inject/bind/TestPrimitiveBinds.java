package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Name.named;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.Injector;

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

		PrimitiveBindsBean( int i, float f, boolean b ) {
			this.i = i;
			this.f = f;
			this.b = b;

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

	@Test
	public void thatPrimitivesWorkAsWrapperClassesWhenInjected() {
		PrimitiveBindsBean bean = injector.resolve( dependency( PrimitiveBindsBean.class ) );
		assertThat( bean.i, is( 42 ) );
		assertThat( bean.f, is( 3.1415f ) );
		assertThat( bean.b, is( true ) );
	}
}
