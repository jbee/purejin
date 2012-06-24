package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import de.jbee.inject.Dependency;
import de.jbee.inject.DependencyResolver;

public class TestPackageLocalisedBinds {

	private static class PackageLocalisedBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( String.class ).to( "default" );
			inPackageOf( TestPackageLocalisedBinds.class ).bind( String.class ).to( "test" );
			inSubPackagesOf( Object.class ).bind( String.class ).to( "java-lang.*" );
			inPackageAndSubPackagesOf( List.class ).bind( String.class ).to( "java-util.*" );
		}

	}

	private static final Dependency<String> stringGlobal = dependency( String.class );

	private final DependencyResolver injector = Bootstrap.injector( PackageLocalisedBindsModule.class );

	@Test
	public void thatDepedencyWithoutTargetResolvedToGlobalBind() {
		assertThat( injector.resolve( stringGlobal ), is( "default" ) );
	}

	@Test
	public void thatDependencyWithTargetResolvedToSpecificBindInThatPackage() {
		Dependency<String> stringInBind = stringGlobal.into( TestPackageLocalisedBinds.class );
		assertThat( injector.resolve( stringInBind ), is( "test" ) );
	}

	@Test
	public void thatDependencyWithTargetSomewhereElseResolvedToGlobalBind() {
		Dependency<String> stringSomewhereElse = stringGlobal.into( java.awt.font.TextAttribute.class );
		assertThat( injector.resolve( stringSomewhereElse ), is( "default" ) );
	}

	@Test
	public void thatDependencyWithTargetResolvedToRelevantSubPackagesBind() {
		Dependency<String> stringInAnnotation = stringGlobal.into( java.lang.annotation.Target.class );
		assertThat( injector.resolve( stringInAnnotation ), is( "java-lang.*" ) );
	}

	@Test
	public void thatDependencyWithTargetResolvedToRelevantPackageOfPackageAndSubPackagesBind() {
		Dependency<String> stringInUtil = stringGlobal.into( java.util.List.class );
		assertThat( injector.resolve( stringInUtil ), is( "java-util.*" ) );
	}

	@Test
	public void thatDependencyWithTargetResolvedToRelevantSubPackageOfPackageAndSubPackagesBind() {
		Dependency<String> stringInUtil = stringGlobal.into( java.util.concurrent.Callable.class );
		assertThat( injector.resolve( stringInUtil ), is( "java-util.*" ) );
	}

}
