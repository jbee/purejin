package de.jbee.inject;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import de.jbee.inject.util.PackageModule;

public class TestInjector {

	static class TestModule
			extends PackageModule {

		@Override
		protected void configure() {
			install( Module.BUILD_IN );
			bind( String.class ).to( "foobar" );
			bind( CharSequence.class ).to( "bar" );
			bind( Integer.class ).to( 42 );
			bind( Float.class ).to( 42.0f );
		}

	}

	@Test
	public void test() {
		Injector injector = Injector.create( new TestModule(), new BuildinModuleBinder() );
		Provider<String> v1 = injector.resolve( Dependency.dependency( Type.rawType( Provider.class ).parametized(
				String.class ) ) );
		System.out.println( v1.yield() );
		System.out.println( Arrays.toString( injector.resolve( Dependency.dependency( Type.rawType( String[].class ) ) ) ) );
		System.out.println( injector.resolve( Dependency.dependency( Type.rawType( CharSequence.class ) ) ) );
		System.out.println( injector.resolve( Dependency.dependency( Type.rawType( List.class ).parametized(
				String.class ) ) ) );
		Provider<List<String>> p2 = injector.resolve( Dependency.dependency( Type.rawType(
				Provider.class ).parametized( Type.rawType( List.class ).parametized( String.class ) ) ) );
		System.out.println( p2.yield() );

		Provider<Set<String>> p3 = injector.resolve( Dependency.dependency( Type.rawType(
				Provider.class ).parametized( Type.rawType( Set.class ).parametized( String.class ) ) ) );
		System.out.println( p3.toString() + " = " + p3.yield() );

		System.out.println( injector.resolve( Dependency.dependency( Type.rawType( List.class ).parametized(
				Integer.class ) ) ) );

		System.out.println( injector.resolve( Dependency.dependency( Type.rawType( List.class ).parametized(
				Number.class ).parametizedAsLowerBounds() ) ) );

		List<List<String>> lls = injector.resolve( Dependency.dependency( Type.rawType( List.class ).parametized(
				Type.rawType( List.class ).parametized( String.class ) ) ) );
		System.out.println( lls );
	}
}
