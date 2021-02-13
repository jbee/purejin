package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.AccessesBy;
import se.jbee.inject.config.NamesBy;
import se.jbee.inject.config.ProducesBy;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.lang.Type.raw;
import static se.jbee.junit.assertion.Assertions.assertEqualsIgnoreOrder;

/**
 * A test that shows usage of {@link Binder.ScopedBinder#autobind()}.
 * <p>
 * As the name 'auto' suggest this is a way of binding {@link
 * java.lang.reflect.Constructor}s, {@link java.lang.reflect.Method}s and {@link
 * java.lang.reflect.Field} in a (semi-) automatic way.
 * <p>
 * The user configures the selection process using strategy interfaces {@link
 * se.jbee.inject.config.ConstructsBy}, {@link ProducesBy}, {@link AccessesBy}
 * to programmatically describe how to find and select the members to use and
 * bind. The created bindings can be further customised using the {@link
 * se.jbee.inject.config.ScopesBy} strategy to decide the {@link
 * se.jbee.inject.Scope} used, and {@link se.jbee.inject.config.HintsBy} to
 * decides what arguments to use for {@link java.lang.reflect.Parameter}s of
 * bound {@link java.lang.reflect.Executable}s.
 * <p>
 * All this strategies are "globally" bound in the {@link se.jbee.inject.Env}
 * simply using {@link Env#with(Class, Object)}. These settings can be adjusted,
 * overridden or replaced per {@link BinderModule} by overriding it {@link
 * BinderModule#configure(Env)} method. Last but not least the strategies can be
 * set individually just for the {@link Binder.ScopedBinder#autobind()} call
 * using its {@link se.jbee.inject.binder.Binder.AutoBinder#produceBy(ProducesBy)}
 * (and similar) methods.
 */
class TestBasicAutoBinds {

	public static class TestBasicAutoBindsModule extends BinderModule {

		public int i = 12;
		public String s = "prefix";
		public long l = 42L;

		@Override
		protected void declare() {
			// share field values for the primitives in this test
			autobind().accessBy(AccessesBy.declaredFields(false)).in(this);

			// bind methods as factories using Hint to find the one we want
			autobind().produceBy(ProducesBy.OPTIMISTIC //
					.parameterTypesStrictlyMatch(raw(String.class), raw(Integer.class))) //
					.in(SelectMethodFromHints.class);

			// bind methods as factories testing OR and OR-ELSE
			ProducesBy flat = ProducesBy.declaredMethods(false);
			autobind().nameBy(NamesBy.DECLARED_NAME) // use method names to avoid clashing binds
					.produceBy(
						flat.returnTypeAssignableTo(raw(boolean.class)) // returning booleans
							.orElse(flat.withModifier(Modifier::isFinal)) // or if they are final
							.or(flat.returnTypeAssignableTo(raw(int.class)))) // as well as any method returning an int
					.in(SelectMethodOrAndOrElse.class);
			// test that explicit scope overrides the ScopedBy
			per(Scope.injection).autobind().produceBy(flat).in(ScopeIsKept.class);
		}
	}

	public static class SelectMethodFromHints {

		public CharSequence fromStringAndInt(String a, Integer b) {
			return a + b;
		}

		public CharSequence fromString(String a) {
			return a;
		}

		public CharSequence fromDoubleAndString(double a, String b) {
			return a + b;
		}

		public CharSequence fromStringAndLongAndInt(int c, String a, long b) {
			return a + b + c;
		}
	}

	public static class SelectMethodOrAndOrElse {

		public boolean boundForBoolean() {
			return true;
		}

		public boolean notBoundForBoolean() {
			return false;
		}

		public int boundForInt() {
			return 2;
		}

		public int alsoBoundForInt() {
			return 3;
		}

		public final double notBound() {
			return -1d;
		}

		public float alsoNotBound() {
			return -2f;
		}
	}

	public static class ScopeIsKept {

		int callCount;

		public byte value() {
			return (byte) callCount++;
		}
	}

	private final Injector context = Bootstrap.injector(
			TestBasicAutoBindsModule.class);

	@Test
	void methodSelectionBySignatureUsingHints() {
		assertEquals("prefix12", context.resolve(CharSequence.class));
	}

	@Test
	void methodSelectionWithOr() {
		assertTrue(context.resolve(boolean.class), "wrong boolean method bound");
		assertEqualsIgnoreOrder(new Integer[] { 2, 3, 12 },
				context.resolve(Integer[].class));
		assertThrows(UnresolvableDependency.class,
				() -> context.resolve(double.class), "should not be bound");
		assertThrows(UnresolvableDependency.class,
				() -> context.resolve(float.class), "should not be bound");
	}

	@Test
	void methodScopeFromExplicitPerClause() {
		assertEquals(0, context.resolve(byte.class).byteValue());
		assertEquals(1, context.resolve(byte.class).byteValue());
		assertEquals(2, context.resolve(byte.class).byteValue());
	}
}
