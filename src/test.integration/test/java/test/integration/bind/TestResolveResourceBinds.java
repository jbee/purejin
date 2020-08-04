package test.integration.bind;

import org.junit.Test;
import se.jbee.inject.*;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import java.util.List;

import static org.junit.Assert.*;
import static se.jbee.inject.Cast.*;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

public class TestResolveResourceBinds {

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
	public void thatResourceIsAvailableForEveryBoundResource() {
		Resource<String> resource = injector.resolve(
				resourceTypeFor(String.class));
		assertNotNull(resource);
		assertEquals("foobar", resource.generate(dependency(String.class)));
	}

	@Test
	public void thatResourceArrayIsAvailableForEveryBoundResource() {
		Resource<String>[] resource = injector.resolve(
				resourcesTypeFor(String.class));
		assertEquals(4, resource.length);
	}

	@Test
	public void thatResourceArrayIsAvailableForAllResources() {
		assertTrue(injector.resolve(Resource[].class).length >= 4);
	}

	@Test
	public void thatResourceArrayFiltersByName() {
		Dependency<? extends Resource<String>[]> dependency = dependency(
				resourcesTypeFor(String.class)).named("special");
		Resource<String>[] rs = injector.resolve(dependency);
		assertEquals(2, rs.length);
		assertEquals("special-list", rs[0].generate(dependency(String.class)));
		assertEquals("special", rs[1].generate(dependency(String.class)));
	}

	@Test
	public void thatGeneratorIsAvailableForEveryBoundResource() {
		Generator<String> generator = injector.resolve(
				generatorTypeOf(raw(String.class)));
		assertNotNull(generator);
		assertEquals("foobar", generator.generate(dependency(String.class)));
	}

	@Test
	public void thatGeneratorArrayIsAvailableForEveryBoundResource() {
		Generator<String>[] gens = injector.resolve(
				generatorsTypeFor(raw(String.class)));
		assertEquals(4, gens.length);
	}

	@Test
	public void thatGeneratorArrayIsAvailableForAllResources() {
		assertTrue(injector.resolve(Generator[].class).length >= 4);
	}

	@Test
	public void thatInjectorIsAvailableByDefault() {
		assertSame(injector, injector.resolve(Injector.class));
	}

	@Test
	public void resolingViaResourceDoesLookupPlainTypeForPotentialMatches() {
		try {
			injector.resolve(named("x"), resourceTypeFor(String.class));
			fail("Expected not to find a matching resource");
		} catch (NoResourceForDependency e) {
			assertFalse(e.getMessage().contains(": none"));
			assertTrue(e.getMessage().contains("special"));
		}
	}

	@Test
	public void resolingViaGeneratorDoesLookupPlainTypeForPotentialMatches() {
		try {
			injector.resolve(named("x"), generatorTypeOf(raw(String.class)));
			fail("Expected not to find a matching resource");
		} catch (NoResourceForDependency e) {
			assertFalse(e.getMessage().contains(": none"));
			assertTrue(e.getMessage().contains("special"));
		}
	}
}
