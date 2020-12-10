package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.Binder.TargetedBinder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import java.nio.LongBuffer;
import java.text.Format;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Packages.subPackagesOf;

/**
 * A test that demonstrates how to overlay general binds in specified packages
 * and/or sub-packages using {@link TargetedBinder#in(se.jbee.inject.Packages)}
 * or any of the utility methods on top of it:
 * {@link TargetedBinder#inPackageOf(Class)},
 * {@link TargetedBinder#inSubPackagesOf(Class)},
 * {@link TargetedBinder#inPackageAndSubPackagesOf(Class)}.
 */
class TestPackageLocalisedBinds {

	private static class PackageLocalisedBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("default");
			inPackageOf(TestPackageLocalisedBinds.class).bind(String.class).to(
					"test");
			inSubPackagesOf(Object.class).bind(String.class).to("java-lang.*");
			inPackageAndSubPackagesOf(List.class).bind(String.class).to(
					"java-util.*");
			in(subPackagesOf(LongBuffer.class, Format.class)).bind(String.class).to(
					"java-nio.* & java-text.*");
		}

	}

	private static final Dependency<String> stringGlobal = dependency(
			String.class);

	private final Injector injector = Bootstrap.injector(
			PackageLocalisedBindsModule.class);

	@Test
	void thatDepedencyWithoutTargetResolvedToGlobalBind() {
		assertEquals("default", injector.resolve(stringGlobal));
	}

	@Test
	void thatDependencyWithTargetResolvedToSpecificBindInThatPackage() {
		Dependency<String> stringInBind = stringGlobal.injectingInto(
				TestPackageLocalisedBinds.class);
		assertEquals("test", injector.resolve(stringInBind));
	}

	@Test
	void thatDependencyWithTargetSomewhereElseResolvedToGlobalBind() {
		Dependency<String> stringSomewhereElse = stringGlobal.injectingInto(
				java.io.Closeable.class);
		assertEquals("default", injector.resolve(stringSomewhereElse));
	}

	@Test
	void thatDependencyWithTargetResolvedToRelevantSubPackagesBind() {
		Dependency<String> stringInAnnotation = stringGlobal.injectingInto(
				java.lang.annotation.Target.class);
		assertEquals("java-lang.*", injector.resolve(stringInAnnotation));
	}

	@Test
	void thatDependencyWithTargetResolvedToRelevantPackageOfPackageAndSubPackagesBind() {
		Dependency<String> stringInUtil = stringGlobal.injectingInto(
				java.util.List.class);
		assertEquals("java-util.*", injector.resolve(stringInUtil));
	}

	@Test
	void thatDependencyWithTargetResolvedToRelevantSubPackageOfPackageAndSubPackagesBind() {
		Dependency<String> stringInUtil = stringGlobal.injectingInto(
				java.util.concurrent.Callable.class);
		assertEquals("java-util.*", injector.resolve(stringInUtil));
	}

	@Test
	void thatDependencyWithTargetResolvedToRelevantMultiSubPackagesBind() {
		Dependency<String> stringInUtil = stringGlobal.injectingInto(
				java.text.spi.NumberFormatProvider.class);
		assertEquals("java-nio.* & java-text.*",
				injector.resolve(stringInUtil));
	}

}
