package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Hint.constant;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Type.raw;

/**
 * The test illustrates how to use {@link Hint}s to give hints which
 * resources should be injected as constructor arguments.
 *
 * Thereby it is important to notice that the list of {@linkplain Hint}s
 * does *not* describe which {@link Constructor} to use! Hence the sequence does
 * *not* has to match the parameter sequence in the constructor definition. As
 * long as types are not assignable a {@linkplain Hint} is tried to used as
 * the next constructor parameter. The first assignable is used.
 *
 * @see TestDependencyHintBinds
 *
 * @author Jan Bernitt (jan@jbee.se)
 *
 */
public class TestConstructorHintBinds {

	public static class Foo {

		final Integer baz;

		public Foo(String bar, Integer baz) {
			this.baz = baz;
			// no use
		}
	}

	public static class Bar {

		final String foo;

		public Bar(String foo, Integer baz) {
			this.foo = foo;
		}
	}

	public static class Baz {

		final String foo;
		final String bar;

		public Baz(String foo, String bar) {
			this.foo = foo;
			this.bar = bar;

		}
	}

	public static class Qux {

		final Serializable value;
		final CharSequence sequence;

		public Qux(Serializable value, CharSequence sequence) {
			this.value = value;
			this.sequence = sequence;
		}
	}

	private static class ParameterConstructorBindsModule extends BinderModule {

		@Override
		protected void declare() {
			Instance<String> y = instance(named("y"), raw(String.class));
			bind(String.class).to("should not be resolved");
			bind(named("x"), String.class).to("x");
			bind(y).to("y");
			bind(Integer.class).to(42);
			bind(Foo.class).toConstructor(Hint.relativeReferenceTo(String.class));
			bind(Bar.class).toConstructor(Hint.relativeReferenceTo(Integer.class), y.asHint());
			bind(Baz.class).toConstructor(y.asHint(), y.asHint());
			bind(CharSequence.class).to(String.class); // should not be used
			bind(Serializable.class).to(Integer.class); // should not be used
			bind(Qux.class).toConstructor(y.asHint().asType(CharSequence.class),
					constant(1980).asType(Number.class));
		}
	}

	private static class FaultyParameterConstructorBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind(Bar.class).toConstructor(Hint.relativeReferenceTo(Float.class));
		}

	}

	private final Injector injector = Bootstrap.injector(
			ParameterConstructorBindsModule.class);

	@Test
	public void thatClassParameterIsArranged() {
		assertNotNull(injector.resolve(Foo.class));
	}

	@Test
	public void thatTypeParameterIsArranged() {
		assertNotNull(injector.resolve(Bar.class));
	}

	@Test
	public void thatInstanceParameterIsArranged() {
		Bar bar = injector.resolve(Bar.class);
		assertEquals("y", bar.foo);
	}

	/**
	 * We can see that {@link Hint#asType(Class)} works because the
	 * instance y would also been assignable to the 1st parameter of type
	 * {@link Serializable} (in {@link Qux}) but since we typed it as a
	 * {@link CharSequence} it no longer is assignable to 1st argument and will
	 * be used as 2nd argument where the as well {@link Serializable}
	 * {@link Number} constant used as 2nd {@link Hint} is used for as the
	 * 1st argument.
	 */
	@Test
	public void thatParameterAsAnotherTypeIsArranged() {
		Qux qux = injector.resolve(Qux.class);
		assertEquals("y", qux.sequence);
	}

	/**
	 * @see #thatParameterAsAnotherTypeIsArranged()
	 */
	@Test
	public void thatConstantParameterIsArranged() {
		Qux qux = injector.resolve(Qux.class);
		assertEquals(1980, qux.value);
	}

	@Test
	public void thatReoccuringTypesAreArrangedAsOccuringAfterAnother() {
		Baz baz = injector.resolve(Baz.class);
		assertEquals("y", baz.foo);
		assertEquals("y", baz.bar, "when x alignment after another is broken");
	}

	@Test
	public void thatParametersNotArrangedThrowsException() {
		assertThrows(InconsistentDeclaration.class,
				() -> Bootstrap.injector(FaultyParameterConstructorBindsModule.class));
	}

}
