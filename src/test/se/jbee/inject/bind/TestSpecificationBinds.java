package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.container.Typecast.specTypeOf;
import static se.jbee.inject.container.Typecast.specsTypeOf;

import java.util.List;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Specification;
import se.jbee.inject.bootstrap.Bootstrap;

public class TestSpecificationBinds {

	private static class SpecificationBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( String.class ).to( "foobar" );
			Name special = named( "special" );
			bind( special, String.class ).to( "special" );
			inPackageOf( List.class ).bind( String.class ).to( "list" );
			inPackageOf( List.class ).bind( special, String.class ).to( "special-list" );
		}

	}

	private final Injector injector = Bootstrap.injector( SpecificationBindsModule.class );

	@Test
	public void thatSpecIsAvailableForEveryBoundResource() {
		Specification<String> spec = injector.resolve( specTypeOf( String.class ) );
		assertNotNull( spec );
		assertEquals( "foobar", spec.generator.instanceFor( dependency( String.class ) ) );
	}

	@Test
	public void thatSpecArrayIsAvailableForEveryBoundResource() {
		Specification<String>[] specs = injector.resolve( specsTypeOf( String.class ) );
		assertEquals( 4, specs.length );
	}

	@Test
	public void thatSpecArrayIsAvailableForAllResources() {
		assertEquals( 4, injector.resolve( Specification[].class ).length );
	}

	@Test
	public void thatSpecArrayFiltersByName() {
		Dependency<? extends Specification<String>[]> dependency = dependency(
				specsTypeOf( String.class ) ).named( "special" );
		Specification<String>[] specs = injector.resolve( dependency );
		assertEquals( 2, specs.length );
		assertEquals( "special-list", specs[0].generator.instanceFor( dependency( String.class ) ) );
		assertEquals( "special", specs[1].generator.instanceFor( dependency( String.class ) ) );
	}

	@Test
	public void thatInjectorIsAvailableByDefault() {
		assertSame( injector, injector.resolve( Injector.class ) );
	}
}
