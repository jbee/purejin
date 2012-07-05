package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;

import de.jbee.inject.DependencyResolver;

public class TestEditionFeatureBinds {

	public static enum MyFeature
			implements Feature<MyFeature> {
		FOO,
		BAR;

		@Override
		public MyFeature featureOf( Class<?> bundleOrModule ) {
			Featured featured = bundleOrModule.getAnnotation( Featured.class );
			return featured == null
				? null
				: featured.value();
		}
	}

	@Target ( ElementType.TYPE )
	@Retention ( RetentionPolicy.RUNTIME )
	public static @interface Featured {

		MyFeature value();
	}

	@Featured ( MyFeature.FOO )
	static class FeaturedBundle
			extends DirectBundle {

		@Override
		protected void bootstrap() {
			install( SomeModule.class );
		}
	}

	static class SomeModule
			extends BinderModule {

		@Override
		protected void declare() {
			multibind( Integer.class ).to( 42 );
		}

	}

	static class RootBundle
			extends DirectBundle {

		@Override
		protected void bootstrap() {
			install( FeaturedBundle.class );
			install( FeaturedModule.class );
		}

	}

	@Featured ( MyFeature.BAR )
	static class FeaturedModule
			extends BinderModule {

		@Override
		protected void declare() {
			multibind( Integer.class ).to( 8 );
		}

	}

	@Test
	public void thatNotFeaturedModulesAreNotInstalled() {
		Edition edition = Bootstrap.edition( MyFeature.FOO );
		DependencyResolver injector = Bootstrap.injector( RootBundle.class, edition );
		assertThat( injector.resolve( dependency( Integer[].class ) ), is( new Integer[] { 42 } ) );
	}

	@Test
	public void thatNotFeaturedBundlesAreNotInstalled() {
		Edition edition = Bootstrap.edition( MyFeature.BAR );
		DependencyResolver injector = Bootstrap.injector( RootBundle.class, edition );
		assertThat( injector.resolve( dependency( Integer[].class ) ), is( new Integer[] { 8 } ) );
	}
}
