package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.lang.Type;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.raw;

/**
 * Using the fluent API of {@link se.jbee.inject.bind.Bundle}s and {@link
 * se.jbee.inject.bind.Module}s creates {@link se.jbee.inject.bind.Binding}s
 * which become {@link ResourceDescriptor} which become {@link Resource}s within
 * the {@link Injector} context.
 * <p>
 * Each {@link Resource} is a {@link Generator} plus its description originating
 * from the {@link se.jbee.inject.bind.Binding} that created it. {@link
 * Resource}s can be resolved themselves as if they had been bound as a
 * consequence of a bound type.
 * <p>
 * So when binding a {@link String} one can resolve {@link
 * Resource#resourceTypeOf(Type)} of {@link String} to access the {@link
 * Resource} that is responsible for generating the actual instance.
 * <p>
 * This feature can be used to "re-resolve" the factories for the arguments of
 * {@link java.lang.reflect.Constructor}s and {@link java.lang.reflect.Method}s
 * reducing the re-occurring work needed to inject these in case these are bound
 * in a {@link Scope} where they are called often.
 * <p>
 * This test verifies that both {@link Resource} and {@link Generator}s can be
 * resolved for every bound type.
 */
class TestFeatureResolveResourceBinds {

	private static class TestFeatureResolveResourceBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("foobar");
			Name special = named("special");
			bind(special, String.class).to("special");
			inPackageOf(List.class).bind(String.class).to("list");
			inPackageOf(List.class).bind(special, String.class).to(
					"special-list");
		}

	}

	private final Injector context = Bootstrap.injector(
			TestFeatureResolveResourceBindsModule.class);

	@Test
	void resourceIsAvailableForEveryBoundResource() {
		Resource<String> resource = context.resolve(
				Resource.resourceTypeOf(String.class));
		assertNotNull(resource);
		assertEquals("foobar", resource.generate(dependency(String.class)));
	}

	@Test
	void resourceArrayIsAvailableForEveryBoundResource() {
		Resource<String>[] resource = context.resolve(
				Resource.resourcesTypeOf(String.class));
		assertEquals(4, resource.length);
	}

	@Test
	void resourceArrayIsAvailableForAllResources() {
		assertTrue(context.resolve(Resource[].class).length >= 4);
	}

	@Test
	void resourceArrayFiltersByName() {
		Dependency<? extends Resource<String>[]> dependency = dependency(
				Resource.resourcesTypeOf(String.class)).named("special");
		Resource<String>[] rs = context.resolve(dependency);
		assertEquals(2, rs.length);
		assertEquals("special-list", rs[0].generate(dependency(String.class)));
		assertEquals("special", rs[1].generate(dependency(String.class)));
	}

	@Test
	void generatorIsAvailableForEveryBoundResource() {
		Generator<String> generator = context.resolve(
				Generator.generatorTypeOf(raw(String.class)));
		assertNotNull(generator);
		assertEquals("foobar", generator.generate(dependency(String.class)));
	}

	@Test
	void generatorArrayIsAvailableForEveryBoundResource() {
		Generator<String>[] gens = context.resolve(
				Generator.generatorsTypeOf(raw(String.class)));
		assertEquals(4, gens.length);
	}

	@Test
	void generatorArrayIsAvailableForAllResources() {
		assertTrue(context.resolve(Generator[].class).length >= 4);
	}

	@Test
	void injectorIsAvailableByDefault() {
		assertSame(context, context.resolve(Injector.class));
	}

	@Test
	void resolingViaResourceDoesLookupPlainTypeForPotentialMatches() {
		assertNoResource(named("x"), Resource.resourceTypeOf(String.class));
	}

	@Test
	void resolingViaGeneratorDoesLookupPlainTypeForPotentialMatches() {
		assertNoResource(named("x"), Generator.generatorTypeOf(raw(String.class)));
	}

	private void assertNoResource(Name name, Type<?> type) {
		Exception ex = assertThrows(
				UnresolvableDependency.ResourceResolutionFailed.class,
				() -> context.resolve(name, type),
				"There should not be a resource for instance named " + name);
		assertFalse(ex.getMessage().contains(": none"));
		assertTrue(ex.getMessage().contains("special"));
	}
}
