package se.jbee.inject.bind;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Packages.packageAndSubPackagesOf;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.bootstrap.Inspect.all;
import static se.jbee.inject.container.Typecast.providerTypeOf;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.jws.WebMethod;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Provider;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Inspect;
import se.jbee.inject.bootstrap.Inspector;
import se.jbee.inject.container.Scoped;

/**
 * This test demonstrates the use of an {@link Inspector} to semi-automatically bind
 * {@link Constructor}s and/or {@link Method}s as 'provider' of an instance.
 *
 * The example uses the {@link Inspect#all()} util as {@link Inspector}. It allows to narrow what is
 * bound automatically. For example {@link Annotation}s can be specified that need to be present.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestInspectorBinds {

	@Target ( { METHOD, PARAMETER } )
	@Retention ( RUNTIME )
	public @interface Resource {

		String value();
	}

	static final StringBuffer STATE = new StringBuffer();

	static class InspectorBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( all().methods() ).inModule();
			bind( all().methods().annotatedWith( WebMethod.class ).namedBy( Resource.class ) ).in(
					InspectorBindsImplementor1.class );
			bind( all().methods().returnTypeAssignableTo( raw( Provider.class ) ) ).in(
					InspectorBindsImplementor2.class );
			bind( all().methods().returnTypeIn( packageAndSubPackagesOf( Injector.class ) ) ).in(
					InspectorBindsImplementor3.class );
			per( Scoped.APPLICATION ).bind( all().methods() ).in(
					new InspectorBindsImplementor4( STATE ) );
			per( Scoped.INJECTION ).bind( all().methods() ).in( FactoryImpl.class );
		}

		static int staticFactoryMethod() {
			return 42;
		}

		static long staticFactoryMethodWithParameters( int factor ) {
			return factor * 2L;
		}
	}

	static class FactoryImpl {

		byte i = 0;

		byte throwAwayInstanceFactoryMethod() {
			return i++;
		}
	}

	static class InspectorBindsImplementor1 {

		@WebMethod
		float instanceFactoryMethod() {
			return 42f;
		}

		@WebMethod
		@Resource ( "twentyone" )
		float instanceFactoryMethodWithName() {
			return 21f;
		}

		@WebMethod
		@Resource ( "Foo" )
		double instanceFactoryMethodWithParameters( @Resource ( "twentyone" ) float factor ) {
			return factor * 2d;
		}

		float shouldNotBeBoundSinceItIsNotAnnotated() {
			return 0f;
		}
	}

	static class InspectorBindsImplementor2 {

		Provider<Boolean> assignableToProvider() {
			return () -> true;
		}

		Character shouldNotBeBoundSinceItDoesNotReturnTypeThatIsAssignableToProvider() {
			return 'N';
		}
	}

	static class InspectorBindsImplementor3 {

		Name typeInProjectPackage() {
			return Name.named( "foobar" );
		}

		String shouldNotBeBoundSinceItDoesNotReturnTypeThatIsInProjectPackage() {
			return "foobar";
		}
	}

	static class InspectorBindsImplementor4 {

		final StringBuffer state;

		InspectorBindsImplementor4( StringBuffer state ) {
			this.state = state;
		}

		StringBuffer valueFromStatefullInspectedObject() {
			return state;
		}
	}

	private final Injector injector = Bootstrap.injector( InspectorBindsModule.class );

	@Test
	public void thatInstanceFactoryMethodIsAvailable() {
		assertEquals( 42f, injector.resolve(float.class).floatValue(), 0.01f );
	}

	@Test
	public void thatStaticFactoryMethodIsAvailable() {
		assertEquals( 42, injector.resolve( int.class).intValue() );
	}

	/**
	 * Through the float parameter annotation {@link Resource} the double producing factory method
	 * gets 21f injected instead of 42f.
	 */
	@Test
	public void thatInstanceFactoryMethodWithParametersIsAvailable() {
		assertEquals( 42d, injector.resolve(double.class), 0.01d );
	}

	@Test
	public void thatStaticFactoryMethodWithParametersIsAvailable() {
		assertEquals( 84L, injector.resolve(long.class).longValue() );
	}

	/**
	 * The provider method
	 * {@link InspectorBindsImplementor1#instanceFactoryMethodWithParameters(float)} uses the
	 * {@link Resource} annotation to specify the name. As a result there is a named
	 * {@link Instance} that can be resolved.
	 */
	@Test
	public void thatNamedWithAnnotationCanBeUsedToGetNamedResources() {
		assertEquals( 42d, injector.resolve("foo", double.class), 0.01d );
	}

	@Test
	public void thatMethodsAreBoundThatAreAssignableToSpecifiedType() {
		assertEquals(true, injector.resolve(providerTypeOf(Boolean.class)).provide());
	}

	@Test ( expected = NoResourceForDependency.class )
	public void thatNoMethodsAreBoundThatAreNotAssignableToSpecifiedType() {
		injector.resolve( Character.class );
	}

	@Test
	public void thatMethodsAreBoundThatAreInSpecifiedPackagesSet() {
		assertEquals( named( "foobar" ), injector.resolve( Name.class ) );
	}

	@Test ( expected = NoResourceForDependency.class )
	public void thatNoMethodsAreBoundThatAreNotInSpecifiedPackagesSet() {
		injector.resolve( String.class );
	}

	@Test
	public void thatMethodsAreBoundToSpecificInstance() {
		assertSame( STATE, injector.resolve( StringBuffer.class ) );
	}

	@Test
	public void thatDeclaredScopeIsUsedForInspectedBindings() {
		byte first = injector.resolve( byte.class );
		byte second = injector.resolve( byte.class );
		assertEquals( first + 1, second );
		byte third = injector.resolve( byte.class );
		assertEquals( second + 1, third );
	}
}
