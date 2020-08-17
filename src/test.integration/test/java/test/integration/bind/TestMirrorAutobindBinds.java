package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import test.integration.util.Resource;
import test.integration.util.WebMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Cast.providerTypeOf;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Packages.packageAndSubPackagesOf;
import static se.jbee.inject.lang.Type.raw;
import static se.jbee.inject.config.HintsBy.noParameters;
import static se.jbee.inject.config.NamesBy.defaultName;
import static se.jbee.inject.config.ProducesBy.allMethods;
import static se.jbee.inject.config.ProducesBy.declaredMethods;

/**
 * This test demonstrates the use of mirrors to semi-automatically bind
 * {@link Constructor}s and/or {@link Method}s as 'providers' of an instance.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestMirrorAutobindBinds {

	static final StringBuffer STATE = new StringBuffer();

	public static class TestMirrorAutobindBindsModule extends BinderModule {

		@Override
		protected void declare() {
			autobind() //
					.produceBy(declaredMethods) //
					.in(this);
			autobind() //
					.produceBy(allMethods.annotatedWith(WebMethod.class)) //
					.nameBy(defaultName.unlessAnnotatedWith(Resource.class)) //
					.hintBy(noParameters.unlessAnnotatedWith(Resource.class)) //
					.in(Implementor1.class);
			autobind() //
					.produceBy(allMethods.returnTypeAssignableTo(
							raw(Provider.class))) //
					.in(Implementor2.class);
			autobind() //
					.produceBy(allMethods.returnTypeIn(
							packageAndSubPackagesOf(Injector.class))) //
					.in(Implementor3.class);
			per(Scope.application).autobind() //
					.produceBy(declaredMethods) //
					.in(new Implementor4(STATE));
			per(Scope.injection).autobind() //
					.produceBy(declaredMethods) //
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
		@Resource("twentyone")
		public float instanceFactoryMethodWithName() {
			return 21f;
		}

		@WebMethod
		@Resource("Foo")
		public double instanceFactoryMethodWithParameters(
				@Resource("twentyone") float factor) {
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

		public StringBuffer valueFromStatefullMirrorObject() {
			return state;
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestMirrorAutobindBindsModule.class);

	@Test
	public void thatInstanceFactoryMethodIsAvailable() {
		assertEquals(42f, injector.resolve(float.class).floatValue(), 0.01f);
	}

	@Test
	public void thatStaticFactoryMethodIsAvailable() {
		assertEquals(42, injector.resolve(int.class).intValue());
	}

	/**
	 * Through the float parameter annotation {@link Resource} the double
	 * producing factory method gets 21f injected instead of 42f.
	 */
	@Test
	public void thatInstanceFactoryMethodWithParametersIsAvailable() {
		assertEquals(42d, injector.resolve(double.class), 0.01d);
	}

	@Test
	public void thatStaticFactoryMethodWithParametersIsAvailable() {
		assertEquals(84L, injector.resolve(long.class).longValue());
	}

	/**
	 * The provider method
	 * {@link Implementor1#instanceFactoryMethodWithParameters(float)} uses the
	 * {@link Resource} annotation to specify the name. As a result there is a
	 * named {@link Instance} that can be resolved.
	 */
	@Test
	public void thatNamedWithAnnotationCanBeUsedToGetNamedResources() {
		assertEquals(42d, injector.resolve("Foo", double.class), 0.01d);
	}

	@Test
	public void thatMethodsAreBoundThatAreAssignableToSpecifiedType() {
		assertEquals(true,
				injector.resolve(providerTypeOf(Boolean.class)).provide());
	}

	@Test
	public void thatNoMethodsAreBoundThatAreNotAssignableToSpecifiedType() {
		assertThrows(NoResourceForDependency.class,
				() -> injector.resolve(Character.class));
	}

	@Test
	public void thatMethodsAreBoundThatAreInSpecifiedPackagesSet() {
		assertEquals(named("foobar"), injector.resolve(Name.class));
	}

	@Test
	public void thatNoMethodsAreBoundThatAreNotInSpecifiedPackagesSet() {
		assertThrows(NoResourceForDependency.class,
				() -> injector.resolve(String.class));
	}

	@Test
	public void thatMethodsAreBoundToSpecificInstance() {
		assertSame(STATE, injector.resolve(StringBuffer.class));
	}

	@Test
	public void thatDeclaredScopeIsUsedForMirrorBindings() {
		byte first = injector.resolve(byte.class);
		byte second = injector.resolve(byte.class);
		assertEquals(first + 1, second);
		byte third = injector.resolve(byte.class);
		assertEquals(second + 1, third);
	}
}
