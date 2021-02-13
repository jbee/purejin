package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.UnresolvableDependency.UnstableDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Installs;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.DefaultFeature;
import se.jbee.lang.Type;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Provider.providerTypeOf;
import static se.jbee.lang.Cast.listTypeOf;
import static se.jbee.lang.Type.raw;

/**
 * {@link Provider}s are a common concept in dependency injection used to inject
 * instances with an unstable {@link Scope} into those with a more stable {@link
 * Scope} without running the risk of accessing stale state.
 * <p>
 * This is simply done by not injecting the unstable type directly into the more
 * stable one but by wrapping it into the {@link Provider} indirection. To
 * access the current valid value when needed {@link Provider#provide()} is
 * called right before the value is needed.
 * <p>
 * In most dependency injection frameworks and libraries the burden to use
 * {@link Provider}s where needed lies upon the user and mistakes simply
 * manifest inconsistent runtime behaviour and the underlying scoping issue has
 * to be identified by the user.
 * <p>
 * To lift this burden purejin has the concept of {@link ScopeLifeCycle}s where
 * {@link Scope}s declare their stability or consistency in relation to each
 * other. This is done as part of the {@link se.jbee.inject.defaults.DefaultScopes}
 * but users can define their own scopes or redefine existing ones with
 * different {@link ScopeLifeCycle}. The {@link Injector} uses the {@link
 * ScopeLifeCycle} information to detect faulty relations at injection.
 * <p>
 * The {@link Provider} can then be used to overcome the limitations and allow
 * injecting unstable dependencies into stable ones. By default this feature
 * is not installed. To enable it install {@link DefaultFeature#PROVIDER}.
 */
class TestFeatureProviderBinds {

	static final DynamicState DYNAMIC_STATE_IN_A = new DynamicState();
	static final DynamicState DYNAMIC_STATE_IN_B = new DynamicState();

	static final Instance<WorkingStateConsumer> A = instance(named("A"),
			raw(WorkingStateConsumer.class));
	static final Instance<WorkingStateConsumer> B = instance(named("B"),
			raw(WorkingStateConsumer.class));

	@Installs(features = DefaultFeature.class)
	private static class TestFeatureProviderBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("foobar");
			bind(named("special"), String.class).to("special");
			bind(CharSequence.class).to("bar");
			bind(Integer.class).to(42);
			bind(named("foo"), Integer.class).to(846);
			bind(Float.class).to(42.0f);
			per(Scope.injection).bind(DynamicState.class).toConstructor();
			construct(FaultyStateConsumer.class);
			construct(WorkingStateConsumer.class);

			injectingInto(A).bind(DynamicState.class).to(DYNAMIC_STATE_IN_A);
			injectingInto(B).bind(DynamicState.class).to(DYNAMIC_STATE_IN_B);
			construct(A);
			construct(B);
		}

	}

	public static class DynamicState {

		public DynamicState() {
			// this is constructed per injection so it is "changing over time"
		}
	}

	public static class FaultyStateConsumer {

		public FaultyStateConsumer(DynamicState state) {
			// using the state directly is faulty since the state changes.
		}
	}

	public static class WorkingStateConsumer {

		final Provider<DynamicState> state;
		final Provider<String[]> strings;

		public WorkingStateConsumer(Provider<DynamicState> state,
				Provider<String[]> strings) {
			this.state = state;
			this.strings = strings;
		}

		DynamicState state() {
			return state.provide();
		}
	}

	private final Injector context = Bootstrap.injector(
			TestFeatureProviderBindsModule.class);

	@Test
	void providersAreAvailableForAnyBoundType() {
		assertInjectsProviderFor("foobar", raw(String.class));
		assertInjectsProviderFor(42, raw(Integer.class));
	}

	@Test
	void providersAreAvailableForAnyNamedBoundType() {
		assertInjectsProviderFor(846, raw(Integer.class), named("foo"));
	}

	@Test
	void providersAreAvailableForArrays() {
		WorkingStateConsumer state = context.resolve(
				WorkingStateConsumer.class);
		assertNotNull(state.strings);
		String[] strings = state.strings.provide();
		assertEquals(2, strings.length);
		assertEquals("foobar", strings[0]);
		assertEquals("special", strings[1]);
	}

	@Test
	void providersAreAvailableForLists() {
		List<String> list = asList("foobar", "special");
		assertInjectsProviderFor(list,
				raw(List.class).parameterized(String.class));
	}

	@Test
	void providersAreAvailableForSets() {
		Set<String> set = new HashSet<>(asList("foobar", "special"));
		assertInjectsProviderFor(set, raw(Set.class).parameterized(String.class));
	}

	@Test
	void providersOvercomeScopingConflicts() {
		assertNotNull(context.resolve(WorkingStateConsumer.class));
	}

	@Test
	void scopingConflictsCauseException() {
		Exception ex = assertThrows(UnstableDependency.class,
				() -> context.resolve(FaultyStateConsumer.class));
		assertEquals("Unstable dependency injection" +
						"\n" + "\t  of: test.integration.bind.TestFeatureProviderBinds.DynamicState scoped injection" +
						"\n" + "\tinto: test.integration.bind.TestFeatureProviderBinds.FaultyStateConsumer scoped application",
				ex.getMessage());
	}

	@Test
	void providersKeepHierarchySoProvidedDependencyIsResolvedAsIfResolvedDirectly() {
		WorkingStateConsumer a = context.resolve(A);
		assertSame(DYNAMIC_STATE_IN_A, a.state());
		WorkingStateConsumer b = context.resolve(B);
		assertNotSame(a, b);
		assertSame(DYNAMIC_STATE_IN_B, b.state());
	}

	@Test
	void providersCanBeCombinedWithOtherBridges() {
		Provider<List<WorkingStateConsumer>> provider = context.resolve(
				providerTypeOf(listTypeOf(WorkingStateConsumer.class)));
		assertNotNull(provider);
		List<WorkingStateConsumer> consumers = provider.provide();
		assertEquals(3, consumers.size());
		assertNotNull(consumers.get(0).state.provide());
		assertNotNull(consumers.get(1).state.provide());
		assertNotNull(consumers.get(2).state.provide());
	}

	@Test
	void providerCanProvidePerInjectionInstanceWithinAnPerApplicationParent() {
		WorkingStateConsumer obj = context.resolve(WorkingStateConsumer.class);
		assertNotNull(obj.state()); // if expiry is a problem this will throw an exception
	}

	private <T> void assertInjectsProviderFor(T expected,
			Type<? extends T> dependencyType) {
		assertInjectsProviderFor(expected, dependencyType, Name.ANY);
	}

	private <T> void assertInjectsProviderFor(T expected,
			Type<? extends T> dependencyType, Name name) {
		Provider<?> provider = context.resolve(name,
				providerTypeOf(dependencyType));
		assertEquals(expected, provider.provide());
	}
}
