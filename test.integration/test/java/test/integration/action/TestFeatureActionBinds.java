package test.integration.action;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.action.Action;
import se.jbee.inject.action.ActionExecutionFailed;
import se.jbee.inject.action.ActionModule;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.ProducesBy;
import se.jbee.lang.Type;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Scope.dependencyInstance;
import static se.jbee.inject.Scope.dependencyType;
import static se.jbee.inject.action.Action.actionTypeOf;
import static se.jbee.lang.Type.raw;

/**
 * Tests the {@link Action} implementation.
 * <p>
 * {@link Action}s are a decoupling concept where input-output processing
 * operations are uniquely identified by their fully generic input and output
 * {@link Type}.
 * <p>
 * The service bean actually implementing a particular {@link Action} as {@link
 * java.lang.reflect.Method} is unknown and unimportant to service users.
 * <p>
 * They access the service {@link Action} they need by resolving the {@link
 * Action} that does the transformation they need. For this concept to scale it
 * is essential that business level actions are modelled with an individual
 * input properties type per business operation. These are simple records of all
 * input values necessary to perform the operation. The actual {@link
 * java.lang.reflect.Method} implementing the {@link Action} is identified by
 * having matching input and output types. They can also have further method
 * parameters, like other services or configurations, which the {@link Injector}
 * resolves and injects when the {@link Action} is called.
 * <p>
 * To add {@link java.lang.reflect.Method}s to the set of methods that should be
 * considered as {@link Action}s these are added using the {@link
 * se.jbee.inject.binder.Binder#connect(ProducesBy)} method with {@link
 * Binder.ConnectTargetBinder#asAction()}. This adds the selected methods to the
 * pool of action implementations as the implementing instance is constructed.
 * <p>
 * Method with a non {@link Void} (or {@code void}) return type should only have
 * 1 implementation which computes the result.
 * <p>
 * Methods with return type {@link Void} (or {@code void}) can have many
 * implementations which all are called similar to a multi-dispatch.
 * <p>
 * Note that in this feature test {@link Action}s are used with simple value
 * types as input and output types as this is enough to verify their behaviour
 * but in an actual application the input type (or indeed output type) of an
 * action method must use a type of type combination that makes it unique which
 * is easiest done by creating a operation specific input parameter type, like a
 * {@code RegisterUserParameters} type.
 */
class TestFeatureActionBinds {

	public static class TestFeatureActionBindsModule extends ActionModule {

		@Override
		protected void declare() {
			construct(MyService.class);
			construct(MyOtherService.class);
			per(dependencyInstance).bind(Name.ANY, HandlerService.class).toConstructor();
			per(dependencyType).bind(GenericService.class).toConstructor();

			ConnectBinder connectAll = connect(ProducesBy.OPTIMISTIC);
			connectAll.inAny(MyService.class).asAction();
			connectAll.inAny(MyOtherService.class).asAction();
			connectAll.inAny(HandlerService.class).asAction();
			connectAll.inAny(GenericService.class).asAction();
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

		int count = 0;

		public void handle(String event) {
			calls++;
		}

		public long count(String query) {
			return count++;
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

	private final Injector context = Bootstrap.injector(
			TestFeatureActionBindsModule.class);

	@Test
	void actionsDecoupleConcreteMethods() {
		Action<Integer, Integer> mul2 = context.resolve(
				actionTypeOf(Integer.class, Integer.class));
		assertNotNull(mul2);
		ensureBeansExist();
		assertEquals(9, mul2.run(3).intValue());
		Action<Number, Integer> negate = context.resolve(
				actionTypeOf(Number.class, Integer.class));
		assertNotNull(mul2);
		assertEquals(-3, negate.run(3).intValue());
		assertEquals(11, mul2.run(4).intValue());
		assertEquals(-7, negate.run(7).intValue());
	}

	@Test
	void exceptionsAreWrappedInActionExecutionFailed() {
		Action<Void, Void> error = context.resolve(
				actionTypeOf(void.class, void.class));
		ensureBeansExist();
		Exception ex = assertThrows(ActionExecutionFailed.class,
				() -> error.run(null));
		assertSame(IllegalStateException.class, ex.getCause().getClass());
	}

	/**
	 * Because of its scope used for the {@link HandlerService} there is one
	 * instance per name used. When only "a" is resolved (and connected) running
	 * the action calls only one instance. After "b" is resolved (and connected)
	 * running the action increments two times as there are now two instances.
	 * Thereby this test shows that further targets are added to an already
	 * resolved {@link Action} instance when they become known due to connection
	 * occurring.
	 */
	@Test
	void actionsDynamicallyAddTargets() {
		Action<String, Void> handler = context.resolve(
				actionTypeOf(String.class, void.class));
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
	void actionsUseRoundRobinWhenMultipleSiteWithNonVoidReturnTypeExist() {
		Action<String, Long> handler = context.resolve(
				actionTypeOf(String.class, long.class));
		assertNotNull(handler);

		HandlerService a = context.resolve("a", HandlerService.class);
		HandlerService b = context.resolve("b", HandlerService.class);
		assertNotSame(a, b, "services should be different instances");

		assertEquals(0, a.count);
		assertEquals(0, b.count);
		handler.run("does not matter");
		assertEquals(1, a.count + b.count);
		handler.run("does not matter");
		assertEquals(1, a.count);
		assertEquals(1, b.count);
		handler.run("does not matter");
		assertEquals(3, a.count + b.count);
		handler.run("does not matter");
		assertEquals(2, a.count);
		assertEquals(2, b.count);
	}

	@Test
	void actionsSubstituteTypeLevelTypeParametersInArgumentTypes() {
		Action<UUID, String> printer = context.resolve(
				actionTypeOf(UUID.class, String.class));
		assertNotNull(printer);
		createGenericServiceUUID();
		UUID expected = UUID.randomUUID();
		assertEquals(expected.toString(), printer.run(expected));
	}

	@Test
	void actionsSubstituteTypeLevelTypeParametersInReturnTypes() {
		Action<String, UUID> parser = context.resolve(
				actionTypeOf(String.class, UUID.class));
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
				raw(GenericService.class).parameterized(UUID.class)));
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
