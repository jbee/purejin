package test.integration.action;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency.NoMethodForDependency;
import se.jbee.inject.action.Action;
import se.jbee.inject.action.ActionModule;
import se.jbee.inject.bootstrap.Bootstrap;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.action.Action.actionTypeOf;
import static se.jbee.inject.config.ProducesBy.declaredMethods;

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

	private static class TestExampleAnnotatedActionsBindsModule
			extends ActionModule {

		@Override
		protected void declare() {
			construct(Bean.class);
			connect(declaredMethods.annotatedWith(Marker.class)) //
					.in(Bean.class).asAction();
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

	private final Injector context = Bootstrap.injector(
			TestExampleAnnotatedActionsBindsModule.class);

	@Test
	void annotatedMethodIsBoundAsAction() {
		Action<Void, Integer> answer = context.resolve(
				actionTypeOf(Void.class, Integer.class));
		assertNotNull(context.resolve(Bean.class)); // force creation of Bean
		assertEquals(42, answer.run(null).intValue());
	}

	@Test
	void notAnnotatedMethodIsNotBoundAsAction() {
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
