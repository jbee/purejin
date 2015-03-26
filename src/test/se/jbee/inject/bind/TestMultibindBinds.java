package se.jbee.inject.bind;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.bind.AssertInjects.assertEqualSets;
import static se.jbee.inject.container.Typecast.listTypeOf;
import static se.jbee.inject.container.Typecast.setTypeOf;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;

public class TestMultibindBinds {

	static final Name foo = named( "foo" );
	static final Name bar = named( "bar" );

	private static class MultibindBindsModule1
			extends BinderModule {

		@Override
		protected void declare() {
			multibind( Integer.class ).to( 1 );
			multibind( foo, Integer.class ).to( 2 );
			multibind( bar, Integer.class ).to( 4 );
		}

	}

	private static class MultibindBindsModule2
			extends BinderModule {

		@Override
		protected void declare() {
			multibind( Integer.class ).to( 11 );
			multibind( foo, Integer.class ).to( 3 );
			multibind( bar, Integer.class ).to( 5 );
		}

	}

	private static class MultibindBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( MultibindBindsModule1.class );
			install( MultibindBindsModule2.class );
			install( BuildinBundle.SET );
			install( BuildinBundle.LIST );
		}

	}

	@Test
	public void thatMultipleNamedElementsCanBeBound() {
		Injector injector = Bootstrap.injector( MultibindBindsBundle.class );
		Integer[] foos = injector.resolve( dependency( Integer[].class ).named( foo ) );
		assertEqualSets( new Integer[] { 2, 3 }, foos );
		Integer[] bars = injector.resolve( dependency( Integer[].class ).named( bar ) );
		assertEqualSets( new Integer[] { 4, 5 }, bars );
		Integer[] defaults = injector.resolve( dependency( Integer[].class ).named( Name.DEFAULT ) );
		assertEqualSets( new Integer[] { 1, 11 }, defaults );
		Integer[] anys = injector.resolve( dependency( Integer[].class ).named( Name.ANY ) );
		assertEqualSets( new Integer[] { 1, 2, 3, 4, 5, 11 }, anys );
	}

	@Test
	public void thatMultipleBoundNamedElementsCanUsedAsList() {
		Injector injector = Bootstrap.injector( MultibindBindsBundle.class );
		List<Integer> foos = injector.resolve( dependency( listTypeOf( Integer.class ) ).named( foo ) );
		assertEqualSets( new Integer[] { 2, 3 }, foos.toArray() );
		List<Integer> bars = injector.resolve( dependency( listTypeOf( Integer.class ) ).named( bar ) );
		assertEqualSets( new Integer[] { 4, 5 }, bars.toArray() );
	}

	@Test
	public void thatMultipleBoundNamedElementsCanUsedAsSet() {
		Injector injector = Bootstrap.injector( MultibindBindsBundle.class );
		Set<Integer> foos = injector.resolve( dependency( setTypeOf( Integer.class ) ).named( foo ) );
		assertEqualSets( new Integer[] { 2, 3 }, foos.toArray() );
		Set<Integer> bars = injector.resolve( dependency( setTypeOf( Integer.class ) ).named( bar ) );
		assertEqualSets( new Integer[] { 4, 5 }, bars.toArray() );
	}

}
