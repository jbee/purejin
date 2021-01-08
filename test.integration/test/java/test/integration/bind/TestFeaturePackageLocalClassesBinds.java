package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.New;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Shows the simply but awesome feature of allowing default and package visible
 * managed instances created from constructors using reflection without using
 * deep reflection ({@link java.lang.reflect.AccessibleObject#setAccessible(boolean)})
 * if adding the following binding to one of the {@link
 * se.jbee.inject.bind.Module}s in the same package:
 *
 * <pre>
 * locally().bind(New.class).to(Constructor::newInstance);
 * </pre>
 * <p>
 * The lambda that is created this way is only used within the package and as it
 * is defined in the package will have access to all classes visible in the
 * package.
 */
class TestFeaturePackageLocalClassesBinds {

	private static class TestFeaturePackageLocalClassesBindsModule extends
			BinderModule {

		@Override
		protected void declare() {
			locally().bind(New.class).to(Constructor::newInstance);
			construct(DefaultVisibleBean.class);
		}
	}

	static class DefaultVisibleBean {

		DefaultVisibleBean() {
			// default visible constructor
		}
	}

	private final Injector context = Bootstrap.injector(
			TestFeaturePackageLocalClassesBindsModule.class);

	@Test
	void localNewBindAllowsCreatingDefaultVisibleBeans() {
		assertNotNull(context.resolve(DefaultVisibleBean.class));
	}
}
