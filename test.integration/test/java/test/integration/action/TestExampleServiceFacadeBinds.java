package test.integration.action;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.action.Action;
import se.jbee.inject.action.ActionModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.ProducesBy;
import se.jbee.lang.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.jbee.inject.action.Action.actionTypeOf;
import static se.jbee.lang.Type.raw;

/**
 * This test example shows how to build an application specific "service layer"
 * on top of the {@link Action} concept. The benefit is that application code
 * does not become dependent on the {@link Action} abstraction which is a
 * specific to the dependency injection library.
 *
 * @see TestExampleCommandFacadeBinds
 */
class TestExampleServiceFacadeBinds {

	/**
	 * Think of this as an application specific service interface that normally
	 * would have been defined within your application code. This is what 'your'
	 * code asks for whereby you don't get any dependencies pointing in
	 * direction of the dependency injection module inside your normal
	 * application code.
	 * <p>
	 * The only place you are coupled to the framework module continues to be
	 * binding code that is in added "on top" of the functional application
	 * code.
	 */
	@FunctionalInterface
	private interface Service<A, B> {

		B calc(A param);
	}

	private static class ServiceModule
			extends ActionModule {

		@Override
		protected void declare() {
			construct(MathService.class);
			construct(MathDependentService.class);
			connect(ProducesBy.OPTIMISTIC).inAny(MathService.class).asAction();
			per(Scope.dependencyType)
					.starbind(Service.class) //
					.toSupplier(ServiceModule::supply);
		}

		/**
		 * This is the adapter between the application specific {@link Service}
		 * interface and {@link Action} which should be avoided within
		 * application code.
		 * <p>
		 * It simply "forwards" the request and resolved the action that has the
		 * same type parameters and wraps that into a service using a method
		 * reference.
		 */
		private static Service<?, ?> supply(Dependency<? super Service<?, ?>> dep,
				Injector context) {
			Type<? super Service<?, ?>> type = dep.type();
			return newService(context.resolve(
					actionTypeOf(type.parameter(0), type.parameter(1))));
		}

		private static <A, B> Service<A, B> newService(Action<A, B> action) {
			return action::run;
		}
	}

	public static class MathService {

		public Long square(Integer value) {
			return value.longValue() * value;
		}
	}

	public static class MathDependentService {

		public final Service<Integer, Long> square;

		public MathDependentService(Service<Integer, Long> square) {
			this.square = square;
		}
	}

	private final Injector context = Bootstrap.injector(ServiceModule.class);

	@Test
	void serviceBecomesUserFacadeForAction() {
		Service<Integer, Long> square = context.resolve(
				serviceTypeOf(Integer.class, Long.class));
		assertNotNull(context.resolve(MathService.class)); // force creation of MathService
		assertEquals(4L, square.calc(2));
	}

	@Test
	void servicesAreInjectedWhenNeeded() {
		MathDependentService service = context.resolve(MathDependentService.class);
		assertNotNull(context.resolve(MathService.class)); // force creation of MathService
		assertEquals(9L, service.square.calc(3));
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <A, B> Type<Service<A, B>> serviceTypeOf(Class<A> in, Class<B> out) {
		return (Type) raw(Service.class).parameterized(in, out);
	}
}
