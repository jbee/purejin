package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Type.raw;

class TestResolveResourceBinds {

	private static class TestResolveResourceBindsModule extends BinderModule {

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

	private final Injector injector = Bootstrap.injector(
			TestResolveResourceBindsModule.class);

	@Test
	void thatResourceIsAvailableForEveryBoundResource() {
		Resource<String> resource = injector.resolve(
				Resource.resourceTypeOf(String.class));
		assertNotNull(resource);
		assertEquals("foobar", resource.generate(dependency(String.class)));
	}

	@Test
	void thatResourceArrayIsAvailableForEveryBoundResource() {
		Resource<String>[] resource = injector.resolve(
				Resource.resourcesTypeOf(String.class));
		assertEquals(4, resource.length);
	}

	@Test
	void thatResourceArrayIsAvailableForAllResources() {
		assertTrue(injector.resolve(Resource[].class).length >= 4);
	}

	@Test
	void thatResourceArrayFiltersByName() {
		Dependency<? extends Resource<String>[]> dependency = dependency(
				Resource.resourcesTypeOf(String.class)).named("special");
		Resource<String>[] rs = injector.resolve(dependency);
		assertEquals(2, rs.length);
		assertEquals("special-list", rs[0].generate(dependency(String.class)));
		assertEquals("special", rs[1].generate(dependency(String.class)));
	}

	@Test
	void thatGeneratorIsAvailableForEveryBoundResource() {
		Generator<String> generator = injector.resolve(
				Generator.generatorTypeOf(raw(String.class)));
		assertNotNull(generator);
		assertEquals("foobar", generator.generate(dependency(String.class)));
	}

	@Test
	void thatGeneratorArrayIsAvailableForEveryBoundResource() {
		Generator<String>[] gens = injector.resolve(
				Generator.generatorsTypeOf(raw(String.class)));
		assertEquals(4, gens.length);
	}

	@Test
	void thatGeneratorArrayIsAvailableForAllResources() {
		assertTrue(injector.resolve(Generator[].class).length >= 4);
	}

	@Test
	void thatInjectorIsAvailableByDefault() {
		assertSame(injector, injector.resolve(Injector.class));
	}

	@Test
	void resolingViaResourceDoesLookupPlainTypeForPotentialMatches() {
		try {
			injector.resolve(named("x"), Resource.resourceTypeOf(String.class));
			fail("Expected not to find a matching resource");
		} catch (NoResourceForDependency e) {
			assertFalse(e.getMessage().contains(": none"));
			assertTrue(e.getMessage().contains("special"));
		}
	}

	@Test
	void resolingViaGeneratorDoesLookupPlainTypeForPotentialMatches() {
		try {
			injector.resolve(named("x"), Generator.generatorTypeOf(raw(String.class)));
			fail("Expected not to find a matching resource");
		} catch (NoResourceForDependency e) {
			assertFalse(e.getMessage().contains(": none"));
			assertTrue(e.getMessage().contains("special"));
		}
	}
}
