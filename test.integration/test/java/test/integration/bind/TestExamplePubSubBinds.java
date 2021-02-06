package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Lift;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BinderModuleWith;
import se.jbee.inject.binder.Installs;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.PublishesBy;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A tests the shows how {@link Lift}s behind {@link Binder#boot(Class)} can
 * be used to automatically wire a pub-sub relation.
 * <p>
 * In the test scenario there is a interface for a {@link Publisher} and a
 * {@link Subscriber}. All 3 services implement {@link Subscriber}. Goal is to
 * have them all {@link Publisher#subscribe(Subscriber)} to the {@link
 * PublisherImpl} implementation. This works both for {@link
 * Publisher#subscribe(Subscriber)} being defined in the interface or only in
 * the {@link PublisherImpl} implementation.
 * <p>
 * This example uses {@link Binder#withPublishedAccess()} to make each of the
 * implementations a {@link Subscriber} that is known within the {@link
 * Injector} context. Another option would be to use {@link
 * Binder#multibind(Class)} and explicitly bind each service as a {@link
 * Subscriber} as well.
 * <p>
 * With all {@link Subscriber}s available as {@link Subscriber[]} the
 * initialisation can be done in two ways. Eager wiring at the end of
 * bootstrapping the {@link Injector} is done using {@link Binder#boot(Class)}
 * (see {@link EagerSolution}). Lazy wiring at the point where the {@link
 * Publisher} is created/resolved is done using {@link Binder#lift(Class)}
 * (see {@link LazySolution}).
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

		final List<Subscriber> subs = new ArrayList<>();

		@Override
		public void subscribe(Subscriber sub) {
			subs.add(sub);
		}

		@Override
		public void publish() {
			subs.forEach(Subscriber::onEvent);
		}
	}

	public static class Service1 implements Subscriber {

		int events;

		@Override
		public void onEvent() {
			events++;
		}

	}

	public static class Service2 implements Subscriber {

		int events;

		@Override
		public void onEvent() {
			events++;
		}
	}

	public static class Service3 implements Subscriber {

		int events;

		@Override
		public void onEvent() {
			events++;
		}
	}

	private static class CommonPartOfSolution
			extends BinderModuleWith<Publisher> {

		@Override
		protected void declare(Publisher value) {
			withPublishedAccess().bind(Service1.class).toProvider(Service1::new);
			withPublishedAccess().bind(Service2.class).toProvider(Service2::new);
			withPublishedAccess().bind(Service3.class).toProvider(Service3::new);

			// since we are using a constant but we want it to be affected by
			// Lift we bind it as a scoped constant
			// the reason for using a constant is just so we can verify in the
			// test that in eager case the subscription is created with the
			// context whereas in lazy case on resolving the publisher
			// in a non-test scenario we would just do:
			// bind(Publisher.class).to(PublisherImpl.class);
			bind(Publisher.class).toScoped(value);
		}
	}

	@Installs(bundles = CommonPartOfSolution.class)
	private static class EagerSolution extends BinderModule {

		@Override
		protected void declare() {
			boot(Publisher.class) //
					.forEach(Subscriber.class, Publisher::subscribe);
		}
	}

	@Installs(bundles = CommonPartOfSolution.class)
	private static class LazySolution extends BinderModule {

		@Override
		protected void declare() {
			lift(Publisher.class) //
					.forEach(Subscriber.class, Publisher::subscribe);
		}
	}

	@Test
	void eagerlyEstablishPubSubRelationOnEndOfBootstrapping() {
		assertSubscribedToPublisher(EagerSolution.class, 3);
	}

	@Test
	void lazilyEstablishPubSubRelation() {
		assertSubscribedToPublisher(LazySolution.class, 0);
	}

	private static void assertSubscribedToPublisher(
			Class<? extends Bundle> bundle, int initiallyExpectedSubscribers) {
		PublisherImpl pub = new PublisherImpl();
		Env env = Bootstrap.DEFAULT_ENV
				.with(Publisher.class, pub)
				.with(PublishesBy.class, PublishesBy.OPTIMISTIC);
		Injector context = Bootstrap.injector(env, bundle);
		// at this point the context is created by not necessarily any instances
		// this is where the difference between eager and lazy shows
		assertEquals(initiallyExpectedSubscribers, pub.subs.size());
		assertSame(pub, context.resolve(PublisherImpl.class));
		assertEquals(3, pub.subs.size(), "lazy did not cause subscription");

		Service1 sub1 = context.resolve(Service1.class);
		Service2 sub2 = context.resolve(Service2.class);
		Service3 sub3 = context.resolve(Service3.class);

		assertEquals(0, sub1.events);
		assertEquals(0, sub2.events);
		assertEquals(0, sub3.events);
		pub.publish();
		assertEquals(1, sub1.events);
		assertEquals(1, sub2.events);
		assertEquals(1, sub3.events);
	}
}
