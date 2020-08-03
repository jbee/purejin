package se.jbee.inject.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import se.jbee.inject.Initialiser;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * Basic example of how to use {@link Initialiser} on a specific type.
 * 
 * In the example the interface {@link MyListener} should be initialised. The
 * classes {@link MyService} and {@link MyServiceExtension} implement the
 * {@link MyListener} interface. Nowhere is stated that the initialisation of
 * {@link MyListener} should run for the particular classes. They do run just
 * because the classes are implementations of the {@link MyListener} interface.
 * 
 * This mechanism can be used to setup more complex relations between managed
 * instances.
 * 
 * @author jan
 * @since 19.1
 */
public class TestDynamicInitialiserBinds {

	private static interface MyListener {

		MyListener inc(int n);
	}

	static class MyService implements MyListener {

		int sum;

		@Override
		public MyListener inc(int n) {
			sum += n;
			return this;
		}

	}

	private static class MyServiceExtension extends MyService {

	}

	private static class MyOtherService {

	}

	private static class DynamicInitialiserBindsModule extends BinderModule {

		@Override
		protected void declare() {
			initbind(MyListener.class).to(
					(Initialiser<MyListener>) (l, injector) -> l.inc(1));
			injectingInto(MyServiceExtension.class) //
					.initbind(MyListener.class) //
					.to((Initialiser<MyListener>) (l, injector) -> l.inc(2));
			construct(MyService.class);
			construct(MyOtherService.class);
			construct(MyServiceExtension.class);
		}
	}

	@Test
	public void thatDynamicInitialisationTookPlace() {
		Injector injector = Bootstrap.injector(
				DynamicInitialiserBindsModule.class);
		assertEquals(1, injector.resolve(MyService.class).sum);
		assertEquals(3, injector.resolve(MyServiceExtension.class).sum);
		assertNotNull(injector.resolve(MyOtherService.class));
	}
}
