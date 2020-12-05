package test.integration.bind;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import se.jbee.inject.*;
import se.jbee.inject.UnresolvableDependency.DependencyCycle;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Environment;
import se.jbee.inject.config.ConstructsBy;
import se.jbee.inject.defaults.DefaultScopes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Cast.resourcesTypeFor;
import static se.jbee.inject.Hint.relativeReferenceTo;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.config.ConstructsBy.common;
import static se.jbee.inject.lang.Type.raw;

/**
 * The tests shows an example of cyclic depended {@link Bundle}s. It shows that
 * a {@link Bundle} doesn't have to know or consider other bundles since it is
 * valid to make cyclic references or install the {@link Bundle}s multiple
 * times.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestBootstrapper {

	/**
	 * One of two bundles in a minimal example of mutual dependent bundles.
	 * While this installs {@link OtherMutualDependentBundle} that bundle itself
	 * installs this bundle. This should not be a problem and both bundles are
	 * just installed once.
	 */
	private static class OneMutualDependentBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(OtherMutualDependentBundle.class);
		}

	}

	private static class OtherMutualDependentBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(OneMutualDependentBundle.class);
		}

	}

	/**
	 * Because the same {@link Locator} is defined twice (the
	 * {@link Name#DEFAULT} {@link Integer} instance) this module should cause
	 * an exception. All {@link Locator} have to be unique.
	 */
	private static class ClashingBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(Integer.class).to(42);
			bind(Integer.class).to(8);
		}

	}

	private static class ReplacingBindsModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault().bind(Number.class).to(7);
			asDefault().bind(Integer.class).to(11);
			autobind(Integer.class).to(2);
			autobind(Float.class).to(4f);
			autobind(Double.class).to(42d);
			bind(Number.class).to(6);
		}

	}

	@SuppressWarnings("unused")
	private static class Foo {

		Foo(Bar bar) {
			// something
		}
	}

	@SuppressWarnings("unused")
	private static class Bar {

		public Bar(Foo foo) {
			// ...
		}
	}

	@SuppressWarnings("unused")
	private static class A {

		A(B b) {
			// ...
		}
	}

	@SuppressWarnings("unused")
	private static class B {

		B(C c) {
			// ...
		}
	}

	@SuppressWarnings("unused")
	private static class C {

		C(A a) {
			// ...
		}
	}

	private static class CyclicBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(Foo.class).toConstructor(relativeReferenceTo(Bar.class));
			bind(Bar.class).toConstructor(relativeReferenceTo(Foo.class));
		}

	}

	private static class CircularBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(A.class).toConstructor(relativeReferenceTo(B.class));
			bind(B.class).toConstructor(relativeReferenceTo(C.class));
			bind(C.class).toConstructor(relativeReferenceTo(A.class));
		}

	}

	private static class EagerSingletonsBindsModule extends BinderModule
			implements Supplier<String> {

		static int eagers = 0;

		@Override
		protected void declare() {
			bindScopePermanence(ScopePermanence.singleton.derive(
					Scope.application).eager());
			bind(named("eager"), String.class).toSupplier(this);
			per(Scope.injection).bind(named("lazy"), String.class).toSupplier(
					this);
		}

		@Override
		public String supply(Dependency<? super String> dep, Injector context) {
			if (!dep.instance.name.equalTo(named("lazy"))) {
				eagers++;
				return "eager";
			}
			fail("since it is lazy it should not be called");
			return "fail";
		}

	}

	private static class CustomMirrorBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(DefaultScopes.class);
			install(CustomMirrorModule.class);
		}
	}

	@Target(ElementType.CONSTRUCTOR)
	@Retention(RetentionPolicy.RUNTIME)
	@interface ConstructFrom {

	}

	private static class CustomMirrorModule extends BinderModule {

		@Override
		protected Env configure(Env env) {
			return Environment.override(env) //
					.with(ConstructsBy.class,
							common.annotatedWith(ConstructFrom.class));
		}

		@Override
		protected void declare() {
			construct(D.class);
			bind(String.class).to("will be passed to D");
		}
	}

	@SuppressWarnings("unused")
	public static class D {

		final String s;

		@ConstructFrom
		public D(String s) {
			this.s = s;

		}

		public D() {
			this("would be picked normally");
		}
	}

	/**
	 * The assert itself doesn't play such huge role here. we just want to reach
	 * this code.
	 */
	@Test
	@Timeout(1)
	void thatBundlesAreNotBootstrappedMultipleTimesEvenWhenTheyAreMutual() {
		assertTimeout(Duration.ofMillis(100),
				() -> Bootstrap.injector(OneMutualDependentBundle.class));
	}

	@Test
	void thatNonUniqueResourcesThrowAnException() {
		assertThrows(InconsistentDeclaration.class,
				() -> Bootstrap.injector(ClashingBindsModule.class));
	}

	@Test
	@Timeout(1)
	public void thatDependencyCyclesAreDetected() {
		Injector injector = Bootstrap.injector(CyclicBindsModule.class);
		assertThrows(DependencyCycle.class, () -> injector.resolve(Foo.class));
	}

	@Test
	@Timeout(1)
	public void thatDependencyCyclesInCirclesAreDetected() {
		Injector injector = Bootstrap.injector(CircularBindsModule.class);
		assertThrows(DependencyCycle.class, () -> injector.resolve(A.class));
	}

	/**
	 * In the example {@link Number} is {@link DeclarationType#AUTO} bound for
	 * {@link Integer} and {@link Float} but an {@link DeclarationType#EXPLICIT}
	 * bind done overrides these automatic binds. They are removed and no
	 * {@link Generator} is created for them.
	 */
	@Test
	public void thatBindingsAreReplacedByMoreQualiedOnes() {
		Injector injector = Bootstrap.injector(ReplacingBindsModule.class);
		assertEquals(6, injector.resolve(Number.class));
		Resource<?>[] rs = injector.resolve(raw(Resource.class).parametized(
				Number.class).parametizedAsUpperBounds().addArrayDimension());
		//TODO can this be limited to cases with a certain Scope so that container can be excluded?
		assertEquals(4, rs.length);
		Resource<Number>[] forNumber = injector.resolve(
				resourcesTypeFor(Number.class));
		assertEquals(1, forNumber.length);
		@SuppressWarnings("rawtypes")
		Resource<Comparable>[] forCompareable = injector.resolve(
				resourcesTypeFor(Comparable.class));
		assertEquals(3, forCompareable.length);
	}

	@Test
	public void thatEagerSingeltonsCanBeCreated() {
		Injector injector = Bootstrap.injector(
				EagerSingletonsBindsModule.class);
		assertNotNull(injector);
		assertEquals(1, EagerSingletonsBindsModule.eagers);
	}

	@Test
	public void thatCustomMirrorIsUsedToPickConstructor() {
		Injector injector = Bootstrap.injector(CustomMirrorBundle.class);
		assertEquals("will be passed to D", injector.resolve(D.class).s);
	}

}
