package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.HintsBy;
import se.jbee.inject.config.NamesBy;
import test.integration.util.Resource;
import test.integration.util.WebMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Packages.packageAndSubPackagesOf;
import static se.jbee.inject.Provider.providerTypeOf;
import static se.jbee.inject.config.ProducesBy.OPTIMISTIC;
import static se.jbee.inject.config.ProducesBy.declaredMethods;
import static se.jbee.lang.Type.raw;

/**
 * This test demonstrates the use of {@link Binder.ScopedBinder#autobind()} in
 * combination with various {@link se.jbee.inject.config.ProducesBy} strategies
 * to semi-automatically bind {@link Constructor}s and/or {@link Method}s as
 * sources of an instances.
 */
class TestBasicCustomAutobindBinds {

	static final StringBuffer STATE = new StringBuffer();

	public static class TestBasicCustomAutobindBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			autobind() //
					.produceBy(declaredMethods(false)) //
					.in(this);
			autobind() //
					.produceBy(OPTIMISTIC.annotatedWith(WebMethod.class)) //
					.nameBy(NamesBy.annotatedWith(Resource.class, Resource::value)) //
					.hintBy(HintsBy.instanceReference( //
							NamesBy.annotatedWith(Resource.class, Resource::value))) //
					.in(Implementor1.class);
			autobind() //
					.produceBy(OPTIMISTIC.returnTypeAssignableTo(
							raw(Provider.class))) //
					.in(Implementor2.class);
			autobind() //
					.produceBy(OPTIMISTIC.returnTypeIn(
							packageAndSubPackagesOf(Injector.class))) //
					.in(Implementor3.class);
			per(Scope.application).autobind() //
					.produceBy(OPTIMISTIC) //
					.in(new Implementor4(STATE));
			per(Scope.injection).autobind() //
					.produceBy(OPTIMISTIC) //
					.in(FactoryImpl.class);
		}

		public static int staticFactoryMethod() {
			return 42;
		}

		public static long staticFactoryMethodWithParameters(int factor) {
			return factor * 2L;
		}
	}

	public static class FactoryImpl {

		byte i = 0;

		public byte throwAwayInstanceFactoryMethod() {
			return i++;
		}
	}

	public static class Implementor1 {

		@WebMethod
		public float instanceFactoryMethod() {
			return 42f;
		}

		@WebMethod
		@Resource("twenty-one")
		public float instanceFactoryMethodWithName() {
			return 21f;
		}

		@WebMethod
		@Resource("Foo")
		public double instanceFactoryMethodWithParameters(
				@Resource("twenty-one") float factor) {
			return factor * 2d;
		}

		public float shouldNotBeBoundSinceItIsNotAnnotated() {
			return 0f;
		}
	}

	public static class Implementor2 {

		public Provider<Boolean> assignableToProvider() {
			return () -> true;
		}

		public Character shouldNotBeBoundSinceItDoesNotReturnTypeThatIsAssignableToProvider() {
			return 'N';
		}
	}

	public static class Implementor3 {

		public Name typeInProjectPackage() {
			return Name.named("foobar");
		}

		public String shouldNotBeBoundSinceItDoesNotReturnTypeThatIsInProjectPackage() {
			return "foobar";
		}
	}

	public static class Implementor4 {

		final StringBuffer state;

		public Implementor4(StringBuffer state) {
			this.state = state;
		}

		public StringBuffer valueFromStatefulMirrorObject() {
			return state;
		}
	}

	private final Injector context = Bootstrap.injector(
			TestBasicCustomAutobindBindsModule.class);

	@Test
	void instanceFactoryMethodIsAvailable() {
		assertEquals(42f, context.resolve(float.class), 0.01f);
	}

	@Test
	void staticFactoryMethodIsAvailable() {
		assertEquals(42, context.resolve(int.class).intValue());
	}

	/**
	 * Through the float parameter annotation {@link Resource} the double
	 * producing factory method gets 21f injected instead of 42f.
	 */
	@Test
	void instanceFactoryMethodWithParametersIsAvailable() {
		assertEquals(42d, context.resolve(double.class), 0.01d);
	}

	@Test
	void staticFactoryMethodWithParametersIsAvailable() {
		assertEquals(84L, context.resolve(long.class).longValue());
	}

	/**
	 * The provider method
	 * {@link Implementor1#instanceFactoryMethodWithParameters(float)} uses the
	 * {@link Resource} annotation to specify the name. As a result there is a
	 * named {@link Instance} that can be resolved.
	 */
	@Test
	void namedWithAnnotationCanBeUsedToGetNamedResources() {
		assertEquals(42d, context.resolve("Foo", double.class), 0.01d);
	}

	@Test
	void methodsAreBoundThatAreAssignableToSpecifiedType() {
		assertEquals(true,
				context.resolve(providerTypeOf(Boolean.class)).provide());
	}

	@Test
	void noMethodsAreBoundThatAreNotAssignableToSpecifiedType() {
		assertThrows(UnresolvableDependency.ResourceResolutionFailed.class,
				() -> context.resolve(Character.class));
	}

	@Test
	void methodsAreBoundThatAreInSpecifiedPackagesSet() {
		assertEquals(named("foobar"), context.resolve(Name.class));
	}

	@Test
	void noMethodsAreBoundThatAreNotInSpecifiedPackagesSet() {
		assertThrows(UnresolvableDependency.ResourceResolutionFailed.class,
				() -> context.resolve(String.class));
	}

	@Test
	void methodsAreBoundToSpecificInstance() {
		assertSame(STATE, context.resolve(StringBuffer.class));
	}

	@Test
	void declaredScopeIsUsedForAutoBindings() {
		byte first = context.resolve(byte.class);
		byte second = context.resolve(byte.class);
		assertEquals(first + 1, second);
		byte third = context.resolve(byte.class);
		assertEquals(second + 1, third);
	}
}
