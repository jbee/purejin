package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Scope;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.lang.Type;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Cast.listTypeOf;
import static se.jbee.lang.Type.raw;

/**
 * Test the feature {@link se.jbee.inject.defaults.DefaultFeature#SELF} which
 * allows to inject the {@link Name} or {@link Type} that the created instance
 * represents within the {@link Injector} context.
 * <p>
 * This allows to get hold of the instance's {@link Name} and full generic
 * {@link Type}.
 * <p>
 * It is also possible to inject the full {@link Dependency} that caused the
 * creation of an instance.
 * <p>
 * All this information can be extracted from the {@link Dependency} itself that
 * resolves the {@link Name}, {@link Type} or {@link Dependency} value.
 * <p>
 * Without question this feature is most useful in building more powerful
 * features on top of others. Within actual application code this might appear
 * useful but should be used with caution as these types of information are
 * specific to dependency injection context and should not exist directly as an
 * application level concept.
 */
class TestFeatureSelfInjectionBinds {

	public static class Foo<T> {

		final Name actualName;
		final Type<? extends Foo<T>> actualType;
		final Dependency<? extends Foo<T>> actualDependency;

		public Foo(Type<? extends Foo<T>> actualType, Name actualName,
				Dependency<? extends Foo<T>> actualDependency) {
			this.actualType = actualType;
			this.actualName = actualName;
			this.actualDependency = actualDependency;
		}
	}

	public static class SuperFoo<T> extends Foo<T> {

		final Foo<String> innerFoo;

		public SuperFoo(Foo<String> innerFoo, Name actualName,
				Type<? extends SuperFoo<T>> actualType,
				Dependency<SuperFoo<T>> actualDependency) {
			super(actualType, actualName, actualDependency);
			this.innerFoo = innerFoo;
		}
	}

	public static class Bar {

		final Foo<List<Integer>> foo;
		final SuperFoo<Double> superFoo;

		public Bar(Foo<List<Integer>> foo, SuperFoo<Double> superFoo) {
			this.foo = foo;
			this.superFoo = superFoo;
		}
	}

	public static class Que extends Foo<BigInteger> {

		final Foo<?> genericFoo;

		public Que(Foo<?> genericFoo, Type<Que> actualType,
				Name actualName,
				Dependency<Que> actualDependency) {
			super(actualType, actualName, actualDependency);
			this.genericFoo = genericFoo;
		}
	}

	/**
	 * The specific binds done here are less important.
	 * They should just create different scenarios that can be tested.
	 */
	private static class TestFeatureSelfInjectionBindsModule extends
			BinderModule {

		@Override
		protected void declare() {
			// make Foo and SuperFoo be created per instance
			per(Scope.dependencyInstance).bind(Name.ANY, Foo.class).toConstructor();
			per(Scope.dependencyInstance).bind(Name.ANY, SuperFoo.class).toConstructor();

			// give the Foo in Bar a name we can check for
			injectingInto(Bar.class).construct("myNameNested", Foo.class);

			// give SuperFoo in Bar a name using a Hint
			bind(Bar.class).toConstructor(
					instance(named("special"), raw(SuperFoo.class)).asHint());

			injectingInto(SuperFoo.class).bind(Foo.class).to("inner", Foo.class);

			construct(Que.class);
			bind(named("x"), Que.class).toConstructor(instance(named("y"),
					raw(Foo.class).parameterized(String.class)).asHint());
		}
	}

	private final Injector context = Bootstrap.injector(
			TestFeatureSelfInjectionBindsModule.class);

	/*
	Type
	 */

	@Test
	void actualTypeFromAdHoc() {
		Foo<?> fooString = context.resolve(
				raw(Foo.class).parameterized(String.class));
		assertSame(String.class, fooString.actualType.parameter(0).rawType);
	}

	@Test
	void actualTypeFromParameterNestedFlatType() {
		assertEquals(listTypeOf(Integer.class),
				context.resolve(Bar.class).foo.actualType.parameter(0));
	}

	@Test
	void actualTypeFromParameterNestedDeepType() {
		assertEquals(raw(SuperFoo.class).parameterized(Double.class),
				context.resolve(Bar.class).superFoo.actualType);
	}

	@Test
	void actualTypeFromParameterNestedFlatWildcardType() {
		assertEquals(raw(Foo.class).parameterizedAsUpperBounds(),
				context.resolve(Que.class).genericFoo.actualType);
	}

	@Test
	void actualTypeFromParameterNestedFlatWildcardTypeWithHintOverload() {
		assertEquals(raw(Foo.class).parameterized(String.class),
				context.resolve("x", Que.class).genericFoo.actualType);
	}

	@Test
	void actualTypeFromParameterDoubleNested() {
		assertEquals(String.class, context.resolve(
				Bar.class).superFoo.innerFoo.actualType.parameter(0).rawType);
	}

	@Test
	void actualTypeFromParameterNestedTypeParameter() {
		assertEquals(raw(Que.class),
				context.resolve("x", Que.class).actualType);
	}

	/*
	Name
	 */

	@Test
	void actualNameFromAdHocInjected() {
		assertEquals(named("ad-hoc"), context.resolve(named("ad-hoc"),
				raw(Foo.class).parameterized(String.class)).actualName);
	}

	@Test
	void actualNameFromToClauseAndHintNested() {
		Bar bar = context.resolve(Bar.class);
		assertEquals(named("myNameNested"), bar.foo.actualName);
		assertEquals(named("special"), bar.superFoo.actualName);
	}

	@Test
	void actualNameFromToClauseDoubleNested() {
		assertEquals(named("inner"),
				context.resolve(Bar.class).superFoo.innerFoo.actualName);
	}

	@Test
	void actualNameFromParameterNestedFlatWildcardType() {
		assertEquals(Name.ANY,
				context.resolve(Que.class).genericFoo.actualName);
	}

	@Test
	void actualNameFromParameterNestedFlatWildcardTypeWithHintOverload() {
		Que que = context.resolve("x", Que.class);
		assertEquals(named("x"), que.actualName);
		assertEquals(named("y"), que.genericFoo.actualName);
	}

	/*
	Dependency
	 */

	@Test
	void actualDependencyFromAdHoc() {
		@SuppressWarnings("rawtypes")
		Type<Foo> type = raw(Foo.class).parameterized(String.class);
		assertSimilar(
				dependency(type.asUpperBound()) //
				.injectingInto(anyOf(raw(Foo.class))), //
				context.resolve(type).actualDependency);
	}

	@Test
	void actualDependencyFromParameterNestedFlatWildcardTypeWithHintOverload() {
		Que que = context.resolve("x", Que.class);
		assertSimilar(dependency(Que.class) //
				.injectingInto(instance(named("x"), raw(Que.class))), //
				que.actualDependency);
	}

	@Test
	void actualDependencyFromParameterNestedDeep() {
		Bar bar = context.resolve(Bar.class);
		assertSimilar(dependency(Type.raw(SuperFoo.class).parameterized(Double.class)) //
						.injectingInto(Bar.class) //
						.injectingInto(anyOf(SuperFoo.class)), //
				bar.superFoo.actualDependency);
	}

	/**
	 * There are lots of details in a {@link Dependency} - to replicate them all
	 * exactly goes beyond what we try to check so we are happy if both have
	 * the same string output.
	 */
	private void assertSimilar(Dependency<?> expected, Dependency<?> actual) {
		assertEquals(expected.toString(), actual.toString());
	}
}
