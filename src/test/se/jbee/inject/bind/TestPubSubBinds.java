package se.jbee.inject.bind;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import se.jbee.inject.Initialiser;
import se.jbee.inject.Injector;
import se.jbee.inject.Supplier;
import se.jbee.inject.bind.Binder.InitBinder;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Bundle;
import se.jbee.inject.bootstrap.Supply;

/**
 * A tests the shows how {@link Initialiser}s behind {@link Binder#init(Class)}
 * can be used to automatically wire a pub-sub relation.
 *
 * In the test scenario there is a interface for a {@link Publisher} and a
 * {@link Subscriber} and 3 {@link Service}s that all also implement
 * {@link Subscriber}. Goal is to have them all
 * {@link Publisher#subscribe(Subscriber)} to the {@link PublisherImpl}
 * implementation. This works both for {@link Publisher#subscribe(Subscriber)}
 * being defined in the interface or only in the {@link PublisherImpl}
 * implementation.
 *
 * The important thing to understand is that wiring is done after the
 * {@link Injector} context has been created. The {@link Initialiser} resolves
 * the target instance (here the {@link PublisherImpl}) and the arguments. In
 * one case we use {@link Binder#multibind(Class)} to explicitly bind all three
 * implementation classes to the {@link Subscriber} interface. In the other
 * example we rely on implicit reference bindings being made. For example
 * {@code SomeService.class} is linked to the same constant supplied when bound
 * to {@link Service}.
 * {@link InitBinder#withEvery(Class, java.util.function.BiConsumer)} fetches
 * all bound instances that do implement the target type (here
 * {@link Subscriber}).
 *
 * In both examples it is important to end up with reference bindings to the
 * very same instance of the 3 {@link Service} implementations. Otherwise the
 * instance resolved and subscribed is not the same instance that is resolved
 * when using the {@link Service}.
 */
public class TestPubSubBinds {

	private static interface Subscriber {

		void onEvent();
	}

	private static interface Publisher {

		public void publish();

		void subscribe(Subscriber sub);
	}

	private static class PublisherImpl implements Publisher {

		private final List<Subscriber> subs = new ArrayList<>();

		@Override
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
			init(PublisherImpl.class).withEvery(Subscriber.class, Publisher::subscribe);
		}

	}

	/**
	 * An alternative way is to use {@link #multibind(Class)} and
	 * {@link InitBinder#withAll(se.jbee.inject.Type, java.util.function.BiConsumer)}.
	 */
	private static class PubSubBindsModule2 extends BinderModule {

		/**
		 * Any of the below could have been in another module
		 */
		@Override
		protected void declare() {
			// a constant
			bind(SomeService.class).to(new SomeService());
			bind(Service.class).to(SomeService.class);
			multibind(Subscriber.class).to(SomeService.class);

			// a reference and implicit constructor
			bind(named("ref"), Service.class).to(AnotherService.class);
			multibind(Subscriber.class).to(AnotherService.class);

			// a predefined (unknown constant) with aid of explicit multibind to Subscriber
			Supplier<PredefinedService> predefined = Supply.constant(new PredefinedService());
			bind(named("pre"), Service.class).to(predefined);
			multibind(Subscriber.class).to(predefined);

			bind(Publisher.class).to(PublisherImpl.class);
			init(PublisherImpl.class).withAll(raw(Subscriber[].class), Publisher::subscribe);
		}

	}

	@Test
	public void withEveryUpperBound() {
		assertSubscribedToPublisher(PubSubBindsModule.class);
	}

	@Test
	public void withAllAndMultibind() {
		assertSubscribedToPublisher(PubSubBindsModule2.class);
	}

	private static void assertSubscribedToPublisher(Class<? extends Bundle> bundle) {
		Injector injector = Bootstrap.injector(bundle);
		Publisher pub = injector.resolve(Publisher.class);
		SomeService sub = injector.resolve(SomeService.class);
		AnotherService sub2 = injector.resolve(AnotherService.class);
		PredefinedService sub3 = (PredefinedService)injector.resolve("pre", Service.class);

		assertFalse(sub.event);
		assertFalse(sub2.event);
		assertFalse(sub3.event);
		pub.publish();
		assertTrue(sub.event);
		assertTrue(sub2.event);
		assertTrue(sub3.event);
	}
}
