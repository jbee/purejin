package se.jbee.inject.bind;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Packages.subPackagesOf;

import java.awt.Canvas;
import java.text.Format;
import java.util.List;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.Binder.TargetedBinder;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * A test that demonstrates how to overlay general binds in specified packages and/or sub-packages
 * using {@link TargetedBinder#in(se.jbee.inject.Packages)} or any of the utility methods on top of
 * it: {@link TargetedBinder#inPackageOf(Class)}, {@link TargetedBinder#inSubPackagesOf(Class)},
 * {@link TargetedBinder#inPackageAndSubPackagesOf(Class)}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestPackageLocalisedBinds {

	private static class PackageLocalisedBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( String.class ).to( "default" );
			inPackageOf( TestPackageLocalisedBinds.class ).bind( String.class ).to( "test" );
			inSubPackagesOf( Object.class ).bind( String.class ).to( "java-lang.*" );
			inPackageAndSubPackagesOf( List.class ).bind( String.class ).to( "java-util.*" );
			in( subPackagesOf( Canvas.class, Format.class ) ).bind( String.class ).to(
					"java-awt.* & java-text.*" );
		}

	}

	private static final Dependency<String> stringGlobal = dependency( String.class );

	private final Injector injector = Bootstrap.injector( PackageLocalisedBindsModule.class );

	@Test
	public void thatDepedencyWithoutTargetResolvedToGlobalBind() {
		assertThat( injector.resolve( stringGlobal ), is( "default" ) );
	}

	@Test
	public void thatDependencyWithTargetResolvedToSpecificBindInThatPackage() {
		Dependency<String> stringInBind = stringGlobal.injectingInto( TestPackageLocalisedBinds.class );
		assertThat( injector.resolve( stringInBind ), is( "test" ) );
	}

	@Test
	public void thatDependencyWithTargetSomewhereElseResolvedToGlobalBind() {
		Dependency<String> stringSomewhereElse = stringGlobal.injectingInto( java.io.Closeable.class );
		assertThat( injector.resolve( stringSomewhereElse ), is( "default" ) );
	}

	@Test
	public void thatDependencyWithTargetResolvedToRelevantSubPackagesBind() {
		Dependency<String> stringInAnnotation = stringGlobal.injectingInto( java.lang.annotation.Target.class );
		assertThat( injector.resolve( stringInAnnotation ), is( "java-lang.*" ) );
	}

	@Test
	public void thatDependencyWithTargetResolvedToRelevantPackageOfPackageAndSubPackagesBind() {
		Dependency<String> stringInUtil = stringGlobal.injectingInto( java.util.List.class );
		assertThat( injector.resolve( stringInUtil ), is( "java-util.*" ) );
	}

	@Test
	public void thatDependencyWithTargetResolvedToRelevantSubPackageOfPackageAndSubPackagesBind() {
		Dependency<String> stringInUtil = stringGlobal.injectingInto( java.util.concurrent.Callable.class );
		assertThat( injector.resolve( stringInUtil ), is( "java-util.*" ) );
	}

	@Test
	public void thatDependencyWithTargetResolvedToRelevantMultiSubPackagesBind() {
		Dependency<String> stringInUtil = stringGlobal.injectingInto( java.text.spi.NumberFormatProvider.class );
		assertThat( injector.resolve( stringInUtil ), is( "java-awt.* & java-text.*" ) );
	}

}
