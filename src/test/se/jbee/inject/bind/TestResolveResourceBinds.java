package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.container.Cast.generatorTypeOf;
import static se.jbee.inject.container.Cast.generatorsTypeFor;
import static se.jbee.inject.container.Cast.resourceTypeFor;
import static se.jbee.inject.container.Cast.resourcesTypeFor;

import java.util.List;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Generator;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Resource;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.bootstrap.Bootstrap;

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
		assertEquals("foobar", resource.yield(dependency(String.class)));
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
		assertEquals("special-list", rs[0].yield(dependency(String.class)));
		assertEquals("special", rs[1].yield(dependency(String.class)));
	}

	@Test
	public void thatGeneratorIsAvailableForEveryBoundResource() {
		Generator<String> generator = injector.resolve(
				generatorTypeOf(raw(String.class)));
		assertNotNull(generator);
		assertEquals("foobar", generator.yield(dependency(String.class)));
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
