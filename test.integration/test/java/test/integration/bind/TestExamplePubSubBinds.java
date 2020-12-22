package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.BuildUp;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BinderModuleWith;
import se.jbee.inject.binder.Installs;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Environment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static se.jbee.inject.Name.named;

/**
 * A tests the shows how {@link BuildUp}s behind {@link Binder#boot(Class)} can
 * be used to automatically wire a pub-sub relation.
 * <p>
 * In the test scenario there is a interface for a {@link Publisher} and a
 * {@link Subscriber} and 3 {@link Service}s that all also implement {@link
 * Subscriber}. Goal is to have them all {@link Publisher#subscribe(Subscriber)}
 * to the {@link PublisherImpl} implementation. This works both for {@link
 * Publisher#subscribe(Subscriber)} being defined in the interface or only in
 * the {@link PublisherImpl} implementation.
 * <p>
 * This example uses {@link Binder#multibind(Class)} to make each of the
 * implementations a {@link Subscriber} that is known within the {@link
 * Injector} context. Another option would be to use {@link
 * Binder#superbind(Class)} to simply bind an implementation to all its
 * supertypes including its interfaces.
 * <p>
 * With all {@link Subscriber}s available as {@link Subscriber[]} the
 * initialisation can be done in two ways. Eager wiring at the end of
 * bootstrapping the {@link Injector} is done using {@link Binder#boot(Class)}
 * (see {@link EagerSolution}). Lazy wiring at the point where the {@link
 * Publisher} is created/resolved is done using {@link Binder#upbind(Class)}
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

	interface Service {
	}

	public static class SomeService implements Service, Subscriber {

		int events;

		@Override
		public void onEvent() {
			events++;
		}

	}

	public static class AnotherService implements Service, Subscriber {

		int events;

		@Override
		public void onEvent() {
			events++;
		}
	}

	public static class PredefinedService implements Service, Subscriber {

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
			bind(named("some"), Service.class).to(SomeService.class);
			bind(named("ref"), Service.class).to(AnotherService.class);
			bind(named("pre"), Service.class).to(PredefinedService.class);

			multibind(Subscriber.class).to(SomeService.class);
			multibind(Subscriber.class).to(AnotherService.class);
			multibind(Subscriber.class).to(PredefinedService.class);

			// since we are using a constant but we want it to be affected by
			// BuildUp we bind it as a scoped constant
			// the reason for using a constant is just so we can verify in the
			// test that in eager case the subscription is created with the
			// context whereas in lazy case on resolving the publisher
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
			upbind(Publisher.class) //
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
		Env env = Environment.DEFAULT.with(Publisher.class, pub);
		Injector context = Bootstrap.injector(env, bundle);
		// at this point the context is created by not necessarily any instances
		// this is where the difference between eager and lazy shows
		assertEquals(initiallyExpectedSubscribers, pub.subs.size());
		assertSame(pub, context.resolve(PublisherImpl.class));
		assertEquals(3, pub.subs.size(), "lazy did not cause subscription");

		SomeService sub1 = context.resolve(SomeService.class);
		AnotherService sub2 = context.resolve(AnotherService.class);
		PredefinedService sub3 = context.resolve(PredefinedService.class);

		assertEquals(0, sub1.events);
		assertEquals(0, sub2.events);
		assertEquals(0, sub3.events);
		pub.publish();
		assertEquals(1, sub1.events);
		assertEquals(1, sub2.events);
		assertEquals(1, sub3.events);
	}
}
