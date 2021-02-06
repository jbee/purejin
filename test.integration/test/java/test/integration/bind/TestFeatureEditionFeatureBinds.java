package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.Bootstrapper;
import se.jbee.inject.bind.Dependent;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.Edition;
import se.jbee.inject.config.Feature;

import java.lang.annotation.*;

import static se.jbee.junit.assertion.Assertions.assertEqualsIgnoreOrder;

/**
 * A test that demonstrates how to use {@link Feature}s and {@link Edition}s to
 * allow composition of different setups without introducing a single
 * if-statement in the binding code.
 *
 * The example also shows how {@link Annotation}s like {@link Featured} can be
 * used to mark {@link se.jbee.inject.bind.Bundle}s or {@link Module}s as a specific
 * {@link Feature}.
 */
class TestFeatureEditionFeatureBinds {

	public enum AnnotatedFeature implements Feature<AnnotatedFeature> {
		FOO, BAR, BAZ;

		@Override
		public AnnotatedFeature featureOf(Class<?> bundleOrModule) {
			Featured featured = bundleOrModule.getAnnotation(Featured.class);
			return featured == null ? null : featured.value();
		}
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Featured {

		AnnotatedFeature value();
	}

	private static class RootBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(FeaturedBundle.class);
			install(FeaturedModule.class);
			install(FeaturedOptionBundle.QUX);
		}

	}

	@Featured(AnnotatedFeature.FOO)
	private static class FeaturedBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(SomeModule.class);
		}
	}

	private static class SomeModule extends BinderModule {

		@Override
		protected void declare() {
			multibind(Integer.class).to(42);
		}

	}

	@Featured(AnnotatedFeature.BAR)
	private static class FeaturedModule extends BinderModule {

		@Override
		protected void declare() {
			multibind(Integer.class).to(8);
		}

	}

	@Featured(AnnotatedFeature.BAZ)
	private enum FeaturedOptionBundle
			implements Dependent<FeaturedOptionBundle> {
		QUX;

		@Override
		public void bootstrap(
				Bootstrapper.DependentBootstrapper<FeaturedOptionBundle> bootstrapper) {
			bootstrapper.installDependentOn(QUX, AnotherModule.class);
		}

	}

	private static class AnotherModule extends BinderModule {

		@Override
		protected void declare() {
			multibind(Integer.class).to(128);
		}

	}

	@Test
	void justTheFeaturedBundleIsInstalled() {
		assertEditionInstalls(Edition.includes(AnnotatedFeature.FOO), 42);
	}

	@Test
	void justTheFeaturedModuleIsInstalled() {
		assertEditionInstalls(Edition.includes(AnnotatedFeature.BAR), 8);
	}

	@Test
	void justTheFeaturedModularBundleIsInstalled() {
		assertEditionInstalls(Edition.includes(AnnotatedFeature.BAZ), 128);
	}

	@Test
	void theFeaturedBundlesAndModulesAreInstalled() {
		assertEditionInstalls(
				Edition.includes(AnnotatedFeature.BAR, AnnotatedFeature.BAZ), 8,
				128);
	}

	private static void assertEditionInstalls(Edition edition,
			Integer... expected) {
		Env env = Bootstrap.DEFAULT_ENV.with(Edition.class, edition);
		Injector injector = Bootstrap.injector(env, RootBundle.class);
		assertEqualsIgnoreOrder(expected, injector.resolve(Integer[].class));
	}

}
