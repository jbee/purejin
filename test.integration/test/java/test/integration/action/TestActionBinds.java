package test.integration.action;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.action.Action;
import se.jbee.inject.action.ActionExecutionFailed;
import se.jbee.inject.action.ActionModule;
import se.jbee.inject.bootstrap.Bootstrap;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Scope.dependencyInstance;
import static se.jbee.inject.Scope.dependencyType;
import static se.jbee.inject.action.Action.actionTypeOf;
import static se.jbee.inject.config.ProducesBy.allMethods;
import static se.jbee.inject.lang.Type.raw;

class TestActionBinds {

	public static class ActionBindsModule extends ActionModule {

		@Override
		protected void declare() {
			construct(MyService.class);
			construct(MyOtherService.class);
			per(dependencyInstance).bind(Name.ANY, HandlerService.class).toConstructor();
			per(dependencyType).bind(GenericService.class).toConstructor();

			ConnectBinder connectAll = connect(allMethods);
			connectAll.in(MyService.class).asAction();
			connectAll.in(MyOtherService.class).asAction();
			connectAll.in(HandlerService.class).asAction();
			connectAll.in(GenericService.class).asAction();
		}

	}

	public static class MyService {

		public Integer negate(Number value) {
			return -value.intValue();
		}

		public Void error() {
			throw new IllegalStateException("This should be wrapped!");
		}
	}

	public static class MyOtherService {

		public int mul2(int value, Action<Float, Integer> service) {
			return value * 2 + service.run(2.8f);
		}

		public int round(float value) {
			return Math.round(value);
		}
	}

	public static class HandlerService {

		static int calls;

		public void handle(String event) {
			calls++;
		}
	}

	public static class GenericService<T> {

		public String print(T obj) {
			return obj.toString();
		}

		@SuppressWarnings("unchecked")
		public T parse(String value) {
			// obviously this isn't going to work with any other class
			// but in the test scenario T is expected to be UUID
			return (T) UUID.fromString(value);
		}
	}

	private final Injector context = Bootstrap.injector(ActionBindsModule.class);

	@Test
	void actionsDecoupleConcreteMethods() {
		Action<Integer, Integer> mul2 = context.resolve(
				actionTypeOf(raw(Integer.class), raw(Integer.class)));
		assertNotNull(mul2);
		ensureBeansExist();
		assertEquals(9, mul2.run(3).intValue());
		Action<Number, Integer> negate = context.resolve(
				actionTypeOf(raw(Number.class), raw(Integer.class)));
		assertNotNull(mul2);
		assertEquals(-3, negate.run(3).intValue());
		assertEquals(11, mul2.run(4).intValue());
		assertEquals(-7, negate.run(7).intValue());
	}

	@Test
	void exceptionsAreWrappedInActionMalfunction() {
		Action<Void, Void> error = context.resolve(
				actionTypeOf(raw(Void.class), raw(Void.class)));
		ensureBeansExist();
		try {
			error.run(null);
			fail("Expected an exception...");
		} catch (ActionExecutionFailed e) {
			assertSame(IllegalStateException.class, e.getCause().getClass());
		}
	}

	/**
	 * Because of its scope the {@link HandlerService} there is one instance per
	 * name used. When only a is resolved (and connected) running the action
	 * calls only one instance. After b is resolved (and connected) running the
	 * action increments two times as there are now two instances. Thereby this
	 * test shows that further targets are added to an already resolved {@link
	 * Action} instance when they become known due to connection occurring.
	 */
	@Test
	void actionsDynamicallyAddTargets() {
		Action<String, Void> handler = context.resolve(
				actionTypeOf(raw(String.class), raw(void.class)));
		assertNotNull(handler);
		HandlerService a = context.resolve("a", HandlerService.class);
		handler.run("a");
		assertEquals(1, HandlerService.calls);
		HandlerService b = context.resolve("b", HandlerService.class);
		assertNotNull(b);
		assertNotSame(a, b, "services should be different instances");
		handler.run("a + b");
		assertEquals(3, HandlerService.calls);
	}

	@Test
	void actionsSubstituteTypeLevelTypeParametersInArgumentTypes() {
		Action<UUID, String> printer = context.resolve(
				actionTypeOf(raw(UUID.class), raw(String.class)));
		assertNotNull(printer);
		createGenericServiceUUID();
		UUID expected = UUID.randomUUID();
		assertEquals(expected.toString(), printer.run(expected));
	}

	@Test
	void actionsSubstituteTypeLevelTypeParametersInReturnTypes() {
		Action<String, UUID> parser = context.resolve(
				actionTypeOf(raw(String.class), raw(UUID.class)));
		assertNotNull(parser);
		createGenericServiceUUID();
		UUID expected = UUID.randomUUID();
		assertEquals(expected, parser.run(expected.toString()));
	}

	/**
	 * For the {@link UUID} actions to work the service providing them must have
	 * been created. This is that.
	 */
	private void createGenericServiceUUID() {
		assertNotNull(context.resolve(
				raw(GenericService.class).parametized(UUID.class)));
	}

	/**
	 * As actions build upon method {@link se.jbee.inject.config.Connector}s
	 * they require the providing beans to exist before the {@link Action} can
	 * be used. However, any {@link Action} can be injected before that.
	 */
	private void ensureBeansExist() {
		assertNotNull(context.resolve(MyService.class));
		assertNotNull(context.resolve(MyOtherService.class));
	}
}
