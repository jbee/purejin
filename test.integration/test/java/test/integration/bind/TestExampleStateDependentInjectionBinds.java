package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.binder.Binder.RootBinder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.HintsBy;
import se.jbee.inject.config.NamesBy;
import se.jbee.inject.defaults.DefaultFeature;
import se.jbee.lang.Type;
import test.integration.util.Resource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Provider.providerTypeOf;
import static se.jbee.inject.config.ProducesBy.OPTIMISTIC;
import static se.jbee.lang.Type.raw;

/**
 * The {@link Injector} context is "immutable" once it is created. This refer to
 * the {@link se.jbee.inject.Resource}s it has to generate instances. Within the
 * {@link Scope}s generated instances "accumulate" as they get resolved.
 * <p>
 * In such an "non-dynamic" concept it might appear that it is hard to do
 * dynamic injection. This is not the case. The core idea is that a {@link
 * Supplier} can use any information available in an {@link Injector} context to
 * decide what value it should return. If a {@link Scope} is chosen like {@link
 * Scope#injection} that resolves the returned result each time it is requested
 * the decision can be made dynamic.
 * <p>
 * In this test this it is shown how to make the value injected dependent on the
 * state of another object.
 * <p>
 * This is a more advanced example that introduces a new type of {@link
 * se.jbee.inject.bind.Module}, the {@link RouterModule}, which adds a new
 * fluent API to make this type of state dependent wiring less repetitive and
 * easier to read and write.
 * <p>
 * In the first scenario we have a {@link Validator} abstraction and with a
 * {@link Strict} and a {@link Permissive} implementation. A {@link
 * ValidationStrength} in another bean is used as the state or setting that
 * controls what actual {@link Validator} should be used (injected).
 */
class TestExampleStateDependentInjectionBinds {

	@FunctionalInterface
	interface Validator {

		boolean valid(String input);
	}

	enum ValidationStrength {
		PERMISSIVE, STRICT
	}

	public static class Permissive implements Validator {

		@Override
		public boolean valid(String input) {
			return true; // just for testing
		}
	}

	public static class Strict implements Validator {

		@Override
		public boolean valid(String input) {
			return false; // just for testing
		}
	}

	public static class StatefulObject {

		private ValidationStrength validationStrength;
		private Integer number;

		/**
		 * Will be detected as service method and thereby used
		 */
		public ValidationStrength getValidationStrength() {
			return validationStrength;
		}

		public void setValidationStrength(
				ValidationStrength validationStrength) {
			this.validationStrength = validationStrength;
		}

		/**
		 * Will be detected as service method and thereby used
		 */
		@Resource("answer")
		public Integer getNumber() {
			return number;
		}

		public void setNumber(Integer number) {
			this.number = number;
		}
	}

	/* Module and Bundle code to setup scenario */

	public static abstract class RouterModule extends BinderModule {

		public <T, C> RouterBinder<T> route(Class<T> type) {
			return route(raw(type));
		}

		public <T, C> RouterBinder<T> route(Type<T> type) {
			return new RouterBinder<>(root, type);
		}
	}

	public static class RouterBinder<T> {

		private final RootBinder binder;
		private final Type<T> type;

		RouterBinder(RootBinder binder, Type<T> type) {
			this.binder = new RootBinder(binder.bind().next());
			this.type = type;
		}

		public <S> void via(Class<S> state) {
			via(raw(state));
		}

		public <S> void via(Type<S> state) {
			binder.per(Scope.injection).bind(type).toSupplier(
					stateDependent(type, Dependency.dependency(state)));
		}
	}

	/**
	 * This is a indirection that resolves a {@link Type} dependent on another
	 * current {@link ValidationStrength} value. This can be understand as a
	 * dynamic <i>name</i> switch so that a call is resolved to different named
	 * instances.
	 */
	public static final class StateDependentSupplier<T, S>
			implements Supplier<T> {

		private final Type<T> type;
		private final Dependency<S> state;

		public StateDependentSupplier(Type<T> type, Dependency<S> state) {
			this.type = type;
			this.state = state;
		}

		@Override
		public T supply(Dependency<? super T> dep, Injector context) {
			final S actualState = context.resolve(state);
			return supply(dep, context, actualState);
		}

		private T supply(Dependency<? super T> dependency, Injector injector,
				final S actualState) {
			final Instance<T> actualToInject = Instance.instance(
					named(actualState), type);
			try {
				return injector.resolve(dependency.onInstance(actualToInject));
			} catch (UnresolvableDependency.ResourceResolutionFailed e) {
				if (actualState != null) { // when not trying default
					return supply(dependency, injector, null); // try default
				}
				throw e;
			}
		}
	}

	/**
	 * This module demonstrates state dependent binds on a low level using
	 * general binds.
	 */
	private static class Solution1 extends BinderModule {

		@Override
		protected void declare() {
			per(Scope.injection).bind(Validator.class).toSupplier(
					stateDependent(Type.raw(Validator.class),
							dependency(ValidationStrength.class)));

			bind(named(ValidationStrength.PERMISSIVE), Validator.class) //
					.to(Permissive.class);
			bind(named(ValidationStrength.STRICT), Validator.class) //
					.to(Strict.class);
			bind(named((Object) null), Validator.class).to(Permissive.class);

			// the below is just *a* example - it is just important to provide the 'value' per injection
			per(Scope.injection).autobind() //
					.produceBy(OPTIMISTIC.returnTypeAssignableTo(
							raw(ValidationStrength.class))) //
					.in(StatefulObject.class);
		}
	}

	public static <T, C> Supplier<T> stateDependent(Type<T> type,
			Dependency<C> state) {
		return new StateDependentSupplier<>(type, state);
	}

	/**
	 * The same as {@link Solution1} using {@link #connect(Class)}s. The
	 * important difference is that it is not required to manually bind to a
	 * {@link ValidationStrength} value.
	 */
	private static class Solution2 extends RouterModule {

		@Override
		protected void declare() {
			route(Validator.class).via(ValidationStrength.class);

			bind(named(ValidationStrength.PERMISSIVE), Validator.class) //
					.to(Permissive.class);
			bind(named(ValidationStrength.STRICT), Validator.class) //
					.to(Strict.class);
			bind(named((Object) null), Validator.class).to(Permissive.class);

			// the below is just *a* example - it is just important to provide the 'value' per injection
			per(Scope.injection).autobind() //
					.produceBy(OPTIMISTIC.returnTypeAssignableTo(
							raw(ValidationStrength.class))) //
					.in(StatefulObject.class);
		}

	}

	/**
	 * Same as {@link Solution2} but instead of an {@link Enum} the state is a
	 * {@link Integer} number.
	 */
	private static class Solution3 extends RouterModule {

		@Override
		protected void declare() {
			route(String.class).via(Integer.class);

			bind(named(42), String.class).to("Now it is 42");
			bind(named(7), String.class).to("Now it is 7");
			bind(named((Object) null), String.class).to("Default is undefined");

			// the below is just *a* example - it is just important to provide the 'value' per injection
			per(Scope.injection).autobind() //
					.produceBy(
							OPTIMISTIC.returnTypeAssignableTo(raw(int.class))) //
					.nameBy(NamesBy.annotatedWith(Resource.class, Resource::value)) //
					.hintBy(HintsBy.instanceReference( //
							NamesBy.annotatedWith(Resource.class, Resource::value))) //
					.in(StatefulObject.class);
		}

	}

	private static class Solution1Bundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(Solution1.class);
			install(DefaultFeature.PROVIDER);
		}
	}

	@Test
	void thatStateChangeIsResolvedToAnotherImplementation() {
		Injector context = Bootstrap.injector(Solution1.class);
		assertStateChangeIsResolvedToAnotherImplementation(context);
	}

	@Test
	void thatStateChangeIsResolvedToAnotherImplementation2() {
		Injector context = Bootstrap.injector(Solution2.class);
		assertStateChangeIsResolvedToAnotherImplementation(context);
	}

	private static void assertStateChangeIsResolvedToAnotherImplementation(
			Injector context) {
		StatefulObject config = context.resolve(StatefulObject.class);
		Validator v = context.resolve(Validator.class);
		String input = "input";
		assertTrue(v.valid(input)); // default was PERMISSIVE
		config.setValidationStrength(ValidationStrength.STRICT);
		v = context.resolve(Validator.class);
		assertFalse(v.valid(input)); // STRICT
		config.setValidationStrength(ValidationStrength.PERMISSIVE);
		v = context.resolve(Validator.class);
		assertTrue(v.valid(input)); // PERMISSIVE
	}

	/**
	 * When using a {@link Provider} we do not need to resolve the {@link
	 * Validator} each time a state change occurs.
	 */
	@Test
	void stateChangeIsVisibleThroughProviderIndirection() {
		Injector context = Bootstrap.injector(Solution1Bundle.class);
		StatefulObject config = context.resolve(StatefulObject.class);
		Provider<Validator> v = context.resolve(
				providerTypeOf(Validator.class));
		String input = "input";
		assertTrue(v.provide().valid(input));
		config.setValidationStrength(ValidationStrength.STRICT);
		assertFalse(v.provide().valid(input));
		config.setValidationStrength(ValidationStrength.PERMISSIVE);
		assertTrue(v.provide().valid(input));
	}

	@Test
	void stateChangeIsResolvedUsingNamedInstances() {
		assertConfigNumberResolvedToStringEnding(null, "undefined");
		assertConfigNumberResolvedToStringEnding(7, "7");
		assertConfigNumberResolvedToStringEnding(42, "42");
		assertConfigNumberResolvedToStringEnding(123, "undefined"); // equal to null
	}

	private static void assertConfigNumberResolvedToStringEnding(
			Integer actualValue, String ending) {
		Injector context = Bootstrap.injector(Solution3.class);
		StatefulObject state = context.resolve(StatefulObject.class);
		state.setNumber(actualValue);
		String v = context.resolve(String.class);
		assertTrue(v.endsWith(ending),
				"assumed to end with " + ending + " but was " + v);
	}
}
