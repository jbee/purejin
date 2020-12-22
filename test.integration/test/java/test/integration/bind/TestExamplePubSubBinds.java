package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.BuildUp;
import se.jbee.inject.Injector;
import se.jbee.inject.Supplier;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.Binder.BootBinder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.lang.Type;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.bind.Bindings.supplyConstant;
import static se.jbee.inject.lang.Type.raw;

/**
 * A tests the shows how {@link BuildUp}s behind {@link Binder#boot(Class)}
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
 * {@link Injector} context has been created. The {@link BuildUp} resolves
 * the target instance (here the {@link PublisherImpl}) and the arguments. In
 * one case we use {@link Binder#multibind(Class)} to explicitly bind all three
 * implementation classes to the {@link Subscriber} interface. In the other
 * example we rely on implicit reference bindings being made. For example
 * {@code SomeService.class} is linked to the same constant supplied when bound
 * to {@link Service}.
 * {@link BootBinder#forAny(Class, java.util.function.BiConsumer)} fetches all
 * bound instances that do implement the target type (here {@link Subscriber}).
 *
 * In both examples it is important to end up with reference bindings to the
 * very same instance of the 3 {@link Service} implementations. Otherwise the
 * instance resolved and subscribed is not the same instance that is resolved
 * when using the {@link Service}.
 *
 * @see TestExamplePostConstructBinds
 */
class TestExamplePubSubBinds {

	@FunctionalInterface
	interface Subscriber {

		void onEvent();
	}

	interface Publisher {

		void publish();

		void subscribe(Subscriber sub);
	}

	public static class PublisherImpl implements Publisher {

		private final List<Subscriber> subs = new ArrayList<>();

		@Override
		public void subscribe(Subscriber sub) {
			subs.add(sub);
		}

		@Override
		public void publish() {
			subs.forEach(Subscriber::onEvent);
		}
	}

	interface Service {
	}

	public static class SomeService implements Service, Subscriber {

		boolean event;

		@Override
		public void onEvent() {
			event = true;
		}

	}

	public static class AnotherService implements Service, Subscriber {

		boolean event;

		@Override
		public void onEvent() {
			event = true;
		}
	}

	public static class PredefinedService implements Service, Subscriber {

		boolean event;

		@Override
		public void onEvent() {
			event = true;
		}
	}

	private static class TestExamplePubSubBindsModule1 extends BinderModule {

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
			Supplier<PredefinedService> predefined = supplyConstant(
					new PredefinedService());
			bind(named("pre"), Service.class).toSupplier(predefined);
			multibind(Subscriber.class).toSupplier(predefined);

			bind(Publisher.class).to(PublisherImpl.class);
			boot(PublisherImpl.class).forAny(Subscriber.class,
					Publisher::subscribe);
		}
	}

	/**
	 * An alternative way is to use {@link #multibind(Class)} and
	 * {@link BootBinder#forEach(Type, java.util.function.BiConsumer)}.
	 */
	private static class TestExamplePubSubBindsModule2 extends BinderModule {

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
			Supplier<PredefinedService> predefined = supplyConstant(
					new PredefinedService());
			bind(named("pre"), Service.class).toSupplier(predefined);
			multibind(Subscriber.class).toSupplier(predefined);

			bind(Publisher.class).to(PublisherImpl.class);
			boot(PublisherImpl.class).forEach(raw(Subscriber[].class),
					Publisher::subscribe);
		}

	}

	@Test
	void withMultibindAndForAny() {
		assertSubscribedToPublisher(TestExamplePubSubBindsModule1.class);
	}

	@Test
	void withMultibindAndForEach() {
		assertSubscribedToPublisher(TestExamplePubSubBindsModule2.class);
	}

	private static void assertSubscribedToPublisher(
			Class<? extends Bundle> bundle) {
		Injector context = Bootstrap.injector(bundle);
		Publisher pub = context.resolve(Publisher.class);
		SomeService sub = context.resolve(SomeService.class);
		AnotherService sub2 = context.resolve(AnotherService.class);
		PredefinedService sub3 = (PredefinedService) context.resolve("pre",
				Service.class);

		assertFalse(sub.event);
		assertFalse(sub2.event);
		assertFalse(sub3.event);
		pub.publish();
		assertTrue(sub.event);
		assertTrue(sub2.event);
		assertTrue(sub3.event);
	}
}
