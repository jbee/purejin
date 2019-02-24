package se.jbee.inject.action;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.action.ActionModule.actionDependency;
import static se.jbee.inject.container.Scoped.DEPENDENCY_TYPE;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.container.Supplier;

/**
 * This test shows how to build an application specific service layer on top of
 * the action concept. The benefit is that application code does not become
 * dependent on the {@link Action} abstraction that is part of the DI aspect and
 * should be transparent.
 *
 * @author jan
 */
public class TestServiceBinds {

	/**
	 * Think of this as an application specific service interface that normally
	 * would have been defined within your application code. This is what 'your'
	 * code asks for whereby you don't get any dependencies pointing in
	 * direction of the DI framework inside your normal application code. The
	 * only place you are coupled to the DI-framework is still in the binding
	 * code that is additional to the application code.
	 */
	private interface Service<P, R> {

		R calc(P param);
	}

	/**
	 * This is an adapter to 'your' application specific service interface
	 * adapting to the {@link Action} interface of the DI-framework. So
	 * internally both are resolvable.
	 */
	private static final class ServiceSupplier
			implements Supplier<Service<?, ?>> {

		@Override
		public Service<?, ?> supply(Dependency<? super Service<?, ?>> dep,
				Injector injector) {
			Type<? super Service<?, ?>> type = dep.type();
			return newService(injector.resolve(
					actionDependency(type.parameter(0), type.parameter(1))));
		}

		private static <I, O> Service<I, O> newService(Action<I, O> action) {
			return new ServiceToActionAdapter<>(action);
		}

		static final class ServiceToActionAdapter<I, O>
				implements Service<I, O> {

			private final Action<I, O> action;

			ServiceToActionAdapter(Action<I, O> action) {
				this.action = action;
			}

			@Override
			public O calc(I input) {
				return action.exec(input);
			}

		}

	}

	private static class ServiceBindsModule extends ActionModule {

		@Override
		protected void declare() {
			bindActionsIn(MathService.class);
			per(DEPENDENCY_TYPE).starbind(Service.class).toSupplier(
					ServiceSupplier.class);
		}

	}

	static class MathService {

		Long square(Integer value) {
			return value.longValue() * value;
		}
	}

	@Test
	public void servicesAreResolvable() {
		Injector injector = Bootstrap.injector(ServiceBindsModule.class);
		@SuppressWarnings("unchecked")
		Service<Integer, Long> square = injector.resolve(
				raw(Service.class).parametized(Integer.class, Long.class));
		assertEquals(4L, square.calc(2).longValue());
	}

}
