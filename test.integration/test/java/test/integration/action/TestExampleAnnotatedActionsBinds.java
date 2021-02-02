package test.integration.action;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency.NoMethodForDependency;
import se.jbee.inject.action.Action;
import se.jbee.inject.action.ActionModule;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.ProducesBy;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.action.Action.actionTypeOf;
import static se.jbee.inject.binder.spi.ConnectorBinder.CONNECT_QUALIFIER;
import static se.jbee.inject.config.ProducesBy.OPTIMISTIC;

/**
 * The simple example that shows how {@link java.lang.reflect.Method}s used as
 * {@link Action} can be selected using a user defined {@link
 * java.lang.annotation.Annotation}.
 */
class TestExampleAnnotatedActionsBinds {

	@Retention(RUNTIME)
	@Target({ METHOD, PARAMETER, FIELD })
	@interface Marker {

	}

	private static class TestExampleAnnotatedActionsBindsModule1
			extends ActionModule {

		@Override
		protected void declare() {
			construct(Bean.class);
			connect(OPTIMISTIC.annotatedWith(Marker.class)) //
					.inAny(Bean.class).asAction();
		}
	}

	/**
	 * This is an alternative solution where the {@link se.jbee.inject.config.ProducesBy}
	 * is not provided as an explicit argument to {@link
	 * se.jbee.inject.binder.Binder#connect(ProducesBy)} but resolved from the
	 * {@link se.jbee.inject.Env} when calling {@link Binder#connect()}.
	 */
	private static class TestExampleAnnotatedActionsBindsModule2
			extends ActionModule {

		@Override
		protected void declare() {
			construct(Bean.class);
			connect().inAny(Bean.class).asAction();
		}
	}

	public static class Bean {

		public int notBoundSinceNotAnnotated() {
			return 13;
		}

		public float notBoundSinceNotAnnotated2() {
			return 13f;
		}

		@Marker
		public int ourAction() {
			return 42;
		}
	}

	@Test
	void annotatedMethodIsBoundAsActionWithLocalSelectorWithLocalSelector() {
		annotatedMethodIsBoundAsAction(Bootstrap.injector(
				TestExampleAnnotatedActionsBindsModule1.class));
	}

	@Test
	void notAnnotatedMethodIsNotBoundAsActionWithLocalSelector() {
		notAnnotatedMethodIsNotBoundAsAction(Bootstrap.injector(
				TestExampleAnnotatedActionsBindsModule1.class));
	}

	@Test
	void annotatedMethodIsBoundAsActionWithGlobalSelector() {
		Env env = Bootstrap.DEFAULT_ENV.with(CONNECT_QUALIFIER,
				ProducesBy.class, OPTIMISTIC.annotatedWith(Marker.class));
		annotatedMethodIsBoundAsAction(Bootstrap.injector(env,
				TestExampleAnnotatedActionsBindsModule2.class));
	}

	@Test
	void notAnnotatedMethodIsNotBoundAsActionWithGlobalSelector() {
		Env env = Bootstrap.DEFAULT_ENV.with(CONNECT_QUALIFIER,
				ProducesBy.class, OPTIMISTIC.annotatedWith(Marker.class));
		notAnnotatedMethodIsNotBoundAsAction(Bootstrap.injector(env,
				TestExampleAnnotatedActionsBindsModule2.class));
	}

	private void annotatedMethodIsBoundAsAction(Injector context) {
		Action<Void, Integer> answer = context.resolve(
				actionTypeOf(Void.class, Integer.class));
		assertNotNull(context.resolve(Bean.class)); // force creation of Bean
		assertEquals(42, answer.run(null).intValue());
	}

	private void notAnnotatedMethodIsNotBoundAsAction(Injector context) {
		Action<Void, Float> answer = context.resolve(
				actionTypeOf(Void.class, float.class));
		assertNotNull(context.resolve(Bean.class)); // force creation of Bean
		Exception ex = assertThrows(NoMethodForDependency.class,
				() -> answer.run(null));
		assertEquals(
				"No method for signature: java.lang.Float <any>(java.lang.Void)",
				ex.getMessage());
	}
}
