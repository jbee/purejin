package se.jbee.inject.bind;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Supplier;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Supply;

public class TestPubSubBinds {

	private static interface Subscriber {

		void onEvent();
	}

	private static interface Publisher {

		public void publish();
	}

	private static class PublisherImpl implements Publisher {

		private final List<Subscriber> subs = new ArrayList<>();

		public void subscribe(Subscriber sub) {
			subs.add(sub);
		}

		@Override
		public void publish() {
			subs.forEach(sub -> sub.onEvent());
		}
	}

	static interface Service { }

	static class SomeService implements Service, Subscriber  {

		boolean event;

		@Override
		public void onEvent() {
			event = true;
		}

	}

	static class AnotherService implements Service, Subscriber {

		boolean event;

		@Override
		public void onEvent() {
			event = true;
		}
	}

	static class PredefinedService implements Service, Subscriber {

		boolean event;

		@Override
		public void onEvent() {
			event = true;
		}
	}

	private static class PubSubBindsModule extends BinderModule {

		/**
		 * Any of the below could have been in another module
		 */
		@Override
		protected void declare() {
			// a constant
			bind(Service.class).to(new SomeService());

			// a reference and implicit constructor
			bind(named("ref"), Service.class).to(AnotherService.class);

			// a predefined (unknown constant) with aid of explicit multibind to Subscriber
			Supplier<PredefinedService> predefined = Supply.constant(new PredefinedService());
			bind(named("pre"), Service.class).to(predefined);
			multibind(Subscriber.class).to(predefined);

			bind(Publisher.class).to(PublisherImpl.class);
			autoconnect(Subscriber.class).via(PublisherImpl::subscribe, PublisherImpl.class);
		}

	}

	@Test
	public void test() {
		Injector injector = Bootstrap.injector(PubSubBindsModule.class);
		Publisher pub = injector.resolve(dependency(Publisher.class));
		SomeService sub = injector.resolve(dependency(SomeService.class));
		AnotherService sub2 = injector.resolve(dependency(AnotherService.class));
		PredefinedService sub3 = (PredefinedService)injector.resolve(dependency(instance(named("pre"), raw(Service.class))));

		assertFalse(sub.event);
		assertFalse(sub2.event);
		assertFalse(sub3.event);
		pub.publish();
		assertTrue(sub.event);
		assertTrue(sub2.event);
		assertTrue(sub3.event);
	}
}
