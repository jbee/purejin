package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Packages.subPackagesOf;

import java.awt.Canvas;
import java.text.Format;
import java.util.List;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.Binder.TargetedBinder;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * A test that demonstrates how to overlay general binds in specified packages
 * and/or sub-packages using {@link TargetedBinder#in(se.jbee.inject.Packages)}
 * or any of the utility methods on top of it:
 * {@link TargetedBinder#inPackageOf(Class)},
 * {@link TargetedBinder#inSubPackagesOf(Class)},
 * {@link TargetedBinder#inPackageAndSubPackagesOf(Class)}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestPackageLocalisedBinds {

	private static class PackageLocalisedBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("default");
			inPackageOf(TestPackageLocalisedBinds.class).bind(String.class).to(
					"test");
			inSubPackagesOf(Object.class).bind(String.class).to("java-lang.*");
			inPackageAndSubPackagesOf(List.class).bind(String.class).to(
					"java-util.*");
			in(subPackagesOf(Canvas.class, Format.class)).bind(String.class).to(
					"java-awt.* & java-text.*");
		}

	}

	private static final Dependency<String> stringGlobal = dependency(
			String.class);

	private final Injector injector = Bootstrap.injector(
			PackageLocalisedBindsModule.class);

	@Test
	public void thatDepedencyWithoutTargetResolvedToGlobalBind() {
		assertEquals("default", injector.resolve(stringGlobal));
	}

	@Test
	public void thatDependencyWithTargetResolvedToSpecificBindInThatPackage() {
		Dependency<String> stringInBind = stringGlobal.injectingInto(
				TestPackageLocalisedBinds.class);
		assertEquals("test", injector.resolve(stringInBind));
	}

	@Test
	public void thatDependencyWithTargetSomewhereElseResolvedToGlobalBind() {
		Dependency<String> stringSomewhereElse = stringGlobal.injectingInto(
				java.io.Closeable.class);
		assertEquals("default", injector.resolve(stringSomewhereElse));
	}

	@Test
	public void thatDependencyWithTargetResolvedToRelevantSubPackagesBind() {
		Dependency<String> stringInAnnotation = stringGlobal.injectingInto(
				java.lang.annotation.Target.class);
		assertEquals("java-lang.*", injector.resolve(stringInAnnotation));
	}

	@Test
	public void thatDependencyWithTargetResolvedToRelevantPackageOfPackageAndSubPackagesBind() {
		Dependency<String> stringInUtil = stringGlobal.injectingInto(
				java.util.List.class);
		assertEquals("java-util.*", injector.resolve(stringInUtil));
	}

	@Test
	public void thatDependencyWithTargetResolvedToRelevantSubPackageOfPackageAndSubPackagesBind() {
		Dependency<String> stringInUtil = stringGlobal.injectingInto(
				java.util.concurrent.Callable.class);
		assertEquals("java-util.*", injector.resolve(stringInUtil));
	}

	@Test
	public void thatDependencyWithTargetResolvedToRelevantMultiSubPackagesBind() {
		Dependency<String> stringInUtil = stringGlobal.injectingInto(
				java.text.spi.NumberFormatProvider.class);
		assertEquals("java-awt.* & java-text.*",
				injector.resolve(stringInUtil));
	}

}
