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
 * A test that demonstrates how to make binds that only affect classes in
 * specific packages and/or sub-packages using {@link TargetedBinder#in(se.jbee.inject.Packages)}
 * or any of the utility methods on top of it: {@link TargetedBinder#inPackageOf(Class)},
 * {@link TargetedBinder#inSubPackagesOf(Class)} and {@link
 * TargetedBinder#inPackageAndSubPackagesOf(Class)}.
 * <p>
 * This technique can be used to either as a measure to avoid unwanted
 * collisions pre-actively or as a way to override a "global" binding so that
 * another value is used within a specific package.
 */
class TestBasicPackageLocalBinds {

	private static class TestBasicPackageLocalBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("default");
			inPackageOf(TestBasicPackageLocalBinds.class).bind(String.class).to(
					"test");
			inSubPackagesOf(Object.class).bind(String.class).to("java-lang.*");
			inPackageAndSubPackagesOf(List.class).bind(String.class).to(
					"java-util.*");
			in(subPackagesOf(LongBuffer.class, Format.class)).bind(
					String.class).to("java-nio.* & java-text.*");
		}

	}

	private static final Dependency<String> stringGlobal = dependency(
			String.class);

	private final Injector context = Bootstrap.injector(
			TestBasicPackageLocalBindsModule.class);

	@Test
	void dependencyWithoutTargetResolvedToGlobalBind() {
		assertEquals("default", context.resolve(stringGlobal));
	}

	@Test
	void dependencyWithTargetResolvedToSpecificBindInThatPackage() {
		Dependency<String> stringInBind = stringGlobal.injectingInto(
				TestBasicPackageLocalBinds.class);
		assertEquals("test", context.resolve(stringInBind));
	}

	@Test
	void dependencyWithTargetSomewhereElseResolvedToGlobalBind() {
		Dependency<String> stringSomewhereElse = stringGlobal.injectingInto(
				java.io.Closeable.class);
		assertEquals("default", context.resolve(stringSomewhereElse));
	}

	@Test
	void dependencyWithTargetResolvedToRelevantSubPackagesBind() {
		Dependency<String> stringInAnnotation = stringGlobal.injectingInto(
				java.lang.annotation.Target.class);
		assertEquals("java-lang.*", context.resolve(stringInAnnotation));
	}

	@Test
	void dependencyWithTargetResolvedToRelevantPackageOfPackageAndSubPackagesBind() {
		Dependency<String> stringInUtil = stringGlobal.injectingInto(
				java.util.List.class);
		assertEquals("java-util.*", context.resolve(stringInUtil));
	}

	@Test
	void dependencyWithTargetResolvedToRelevantSubPackageOfPackageAndSubPackagesBind() {
		Dependency<String> stringInUtil = stringGlobal.injectingInto(
				java.util.concurrent.Callable.class);
		assertEquals("java-util.*", context.resolve(stringInUtil));
	}

	@Test
	void dependencyWithTargetResolvedToRelevantMultiSubPackagesBind() {
		Dependency<String> stringInUtil = stringGlobal.injectingInto(
				java.text.spi.NumberFormatProvider.class);
		assertEquals("java-nio.* & java-text.*", context.resolve(stringInUtil));
	}
}
