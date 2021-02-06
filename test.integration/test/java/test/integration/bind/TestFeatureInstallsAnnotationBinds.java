package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Installs;
import se.jbee.inject.bootstrap.Bootstrap;

import static se.jbee.junit.assertion.Assertions.assertEqualsIgnoreOrder;

/**
 * Just a short example that shows that the {@link Installs} annotation while
 * being inherited is not effectively inherited when another {@link Installs}
 * annotation is present on a subtype. In such case the annotation is overridden
 * by that instance of the annotation.
 * <p>
 * Hence, {@link Installs} should only be used on leaf classes that are not
 * subject to further inheritance. To install a {@link se.jbee.inject.bind.Bundle}
 * as a "side-effect" of a {@link se.jbee.inject.bind.Module} use the {@link
 * BinderModule#BinderModule(Class)} constructor.
 */
class TestFeatureInstallsAnnotationBinds {

	@Installs(bundles = A.class)
	abstract static class RootModule extends BinderModule {

	}

	@Installs(bundles = B.class)
	abstract static class NodeModule extends RootModule {

	}

	static class LeafModule extends NodeModule {

		@Override
		protected void declare() {
			multibind(String.class).to("C");
		}
	}

	static class A extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("A");
		}
	}

	static class B extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("B");
		}
	}

	private final Injector context = Bootstrap.injector(LeafModule.class);

	@Test
	void installsOnSupertypesAreOverridden() {
		assertEqualsIgnoreOrder(new String[] { "B", "C" },
				context.resolve(String[].class));
	}
}
