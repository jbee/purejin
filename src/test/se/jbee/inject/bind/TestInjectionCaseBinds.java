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
import static se.jbee.inject.container.Cast.injectionCaseTypeFor;
import static se.jbee.inject.container.Cast.injectionCasesTypeFor;

import java.util.List;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Generator;
import se.jbee.inject.InjectionCase;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.UnresolvableDependency.NoCaseForDependency;
import se.jbee.inject.bootstrap.Bootstrap;

public class TestInjectionCaseBinds {

	private static class TestInjectionCaseBindsModule extends BinderModule {

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
			TestInjectionCaseBindsModule.class);

	@Test
	public void thatInjectionCaseIsAvailableForEveryBoundResource() {
		InjectionCase<String> icase = injector.resolve(
				injectionCaseTypeFor(String.class));
		assertNotNull(icase);
		assertEquals("foobar", icase.yield(dependency(String.class)));
	}

	@Test
	public void thatInjectionCaseArrayIsAvailableForEveryBoundResource() {
		InjectionCase<String>[] icase = injector.resolve(
				injectionCasesTypeFor(String.class));
		assertEquals(4, icase.length);
	}

	@Test
	public void thatInjectionCaseArrayIsAvailableForAllResources() {
		assertTrue(injector.resolve(InjectionCase[].class).length >= 4);
	}

	@Test
	public void thatInjectionCaseArrayFiltersByName() {
		Dependency<? extends InjectionCase<String>[]> dependency = dependency(
				injectionCasesTypeFor(String.class)).named("special");
		InjectionCase<String>[] cases = injector.resolve(dependency);
		assertEquals(2, cases.length);
		assertEquals("special-list", cases[0].yield(dependency(String.class)));
		assertEquals("special", cases[1].yield(dependency(String.class)));
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
	public void resolingViaInjectionCaseDoesLookupPlainTypeForPotentialMatches() {
		try {
			injector.resolve(named("x"), injectionCaseTypeFor(String.class));
			fail("Expected not to find a matching case");
		} catch (NoCaseForDependency e) {
			assertFalse(e.getMessage().contains(": none"));
			assertTrue(e.getMessage().contains("special"));
		}
	}

	@Test
	public void resolingViaGeneratorDoesLookupPlainTypeForPotentialMatches() {
		try {
			injector.resolve(named("x"), generatorTypeOf(raw(String.class)));
			fail("Expected not to find a matching case");
		} catch (NoCaseForDependency e) {
			assertFalse(e.getMessage().contains(": none"));
			assertTrue(e.getMessage().contains("special"));
		}
	}
}
