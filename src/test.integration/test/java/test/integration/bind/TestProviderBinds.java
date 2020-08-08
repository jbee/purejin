package test.integration.bind;

import org.junit.Test;
import se.jbee.inject.*;
import se.jbee.inject.UnresolvableDependency.UnstableDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.CoreFeature;
import se.jbee.inject.lang.Type;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static se.jbee.inject.Cast.listTypeOf;
import static se.jbee.inject.Cast.providerTypeOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Type.raw;

public class TestProviderBinds {

	static final DynamicState DYNAMIC_STATE_IN_A = new DynamicState();
	static final DynamicState DYNAMIC_STATE_IN_B = new DynamicState();

	static final Instance<WorkingStateConsumer> A = instance(named("A"),
			raw(WorkingStateConsumer.class));
	static final Instance<WorkingStateConsumer> B = instance(named("B"),
			raw(WorkingStateConsumer.class));

	private static class ProviderBindsModule extends BinderModule {

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

	private static class ProviderBindsBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			installAll(CoreFeature.class);
			install(ProviderBindsModule.class);
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

	private final Injector injector = Bootstrap.injector(
			ProviderBindsBundle.class);

	@Test
	public void providersAreAvailableForAnyBoundType() {
		assertInjectsProviderFor("foobar", raw(String.class));
		assertInjectsProviderFor(42, raw(Integer.class));
	}

	@Test
	public void providersAreAvailableForAnyNamedBoundType() {
		assertInjectsProviderFor(846, raw(Integer.class), named("foo"));
	}

	@Test
	public void providersAreAvailableForArrays() {
		WorkingStateConsumer state = injector.resolve(
				WorkingStateConsumer.class);
		assertNotNull(state.strings);
		String[] strings = state.strings.provide();
		assertEquals(2, strings.length);
		assertEquals("foobar", strings[0]);
		assertEquals("special", strings[1]);
	}

	@Test
	public void providersAreAvailableForLists() {
		List<String> list = asList("foobar", "special");
		assertInjectsProviderFor(list,
				raw(List.class).parametized(String.class));
	}

	@Test
	public void providersAreAvailableForSets() {
		Set<String> set = new HashSet<>(asList("foobar", "special"));
		assertInjectsProviderFor(set, raw(Set.class).parametized(String.class));
	}

	@Test
	public void providersOvercomeScopingConflicts() {
		assertNotNull(injector.resolve(WorkingStateConsumer.class));
	}

	@Test
	public void scopingConflictsCauseException() {
		try {
			injector.resolve(FaultyStateConsumer.class);
			fail("expected " + UnstableDependency.class.getSimpleName());
		} catch (UnstableDependency e) {
			assertEquals("Unstable dependency injection" +
							"\n" + "\t  of: test.integration.bind.TestProviderBinds.DynamicState  in * into * => *  scoped injection" +
							"\n" + "\tinto: test.integration.bind.TestProviderBinds.FaultyStateConsumer  in * into * => *  scoped application",
					e.getMessage());
		}
	}

	@Test
	public void providersKeepHierarchySoProvidedDependencyIsResolvedAsIfResolvedDirectly() {
		WorkingStateConsumer a = injector.resolve(A);
		assertSame(DYNAMIC_STATE_IN_A, a.state());
		WorkingStateConsumer b = injector.resolve(B);
		assertNotSame(a, b);
		assertSame(DYNAMIC_STATE_IN_B, b.state());
	}

	@Test
	public void providersCanBeCombinedWithOtherBridges() {
		Provider<List<WorkingStateConsumer>> provider = injector.resolve(
				providerTypeOf(listTypeOf(WorkingStateConsumer.class)));
		assertNotNull(provider);
		List<WorkingStateConsumer> consumers = provider.provide();
		assertEquals(3, consumers.size());
		assertNotNull(consumers.get(0).state.provide());
		assertNotNull(consumers.get(1).state.provide());
		assertNotNull(consumers.get(2).state.provide());
	}

	@Test
	public void providerCanProvidePerInjectionInstanceWithinAnPerApplicationParent() {
		WorkingStateConsumer obj = injector.resolve(WorkingStateConsumer.class);
		assertNotNull(obj.state()); // if expiry is a problem this will throw an exception
	}

	private <T> void assertInjectsProviderFor(T expected,
			Type<? extends T> dependencyType) {
		assertInjectsProviderFor(expected, dependencyType, Name.ANY);
	}

	private <T> void assertInjectsProviderFor(T expected,
			Type<? extends T> dependencyType, Name name) {
		Provider<?> provider = injector.resolve(name,
				providerTypeOf(dependencyType));
		assertEquals(expected, provider.provide());
	}
}
