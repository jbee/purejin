package se.jbee.inject.bind;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Packages.packageAndSubPackagesOf;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.bind.Inspect.all;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.hamcrest.Factory;
import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.DIRuntimeException.NoSuchResourceException;
import se.jbee.inject.util.Provider;
import se.jbee.inject.util.Typecast;

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
	public static @interface Resource {

		String value();
	}

	static class InspectorBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( all().methods() ).inModule();
			bind( all().methods().annotatedWith( Factory.class ).namedBy( Resource.class ) ).in(
					InspectorBindsImplementor1.class );
			bind( all().methods().returnTypeAssignableTo( raw( Provider.class ) ) ).in(
					InspectorBindsImplementor2.class );
			bind( all().methods().returnTypeIn( packageAndSubPackagesOf( Injector.class ) ) ).in(
					InspectorBindsImplementor3.class );
		}

		static int staticFactoryMethod() {
			return 42;
		}

		static long staticFactoryMethodWithParameters( int factor ) {
			return factor * 2L;
		}
	}

	static class InspectorBindsImplementor1 {

		@Factory
		float instanceFactoryMethod() {
			return 42f;
		}

		@Factory
		@Resource ( "twentyone" )
		float instanceFactoryMethodWithName() {
			return 21f;
		}

		@Factory
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
			return new Provider<Boolean>() {

				@Override
				public Boolean provide() {
					return true;
				}
			};
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

	private final Injector injector = Bootstrap.injector( InspectorBindsModule.class );

	@Test
	public void thatInstanceFactoryMethodIsAvailable() {
		assertEquals( 42f, injector.resolve( dependency( float.class ) ).floatValue(), 0.01f );
	}

	@Test
	public void thatStaticFactoryMethodIsAvailable() {
		assertEquals( 42, injector.resolve( dependency( int.class ) ).intValue() );
	}

	/**
	 * Through the float parameter annotation {@link Resource} the double producing factory method
	 * gets 21f injected instead of 42f.
	 */
	@Test
	public void thatInstanceFactoryMethodWithParametersIsAvailable() {
		assertEquals( 42d, injector.resolve( dependency( double.class ) ).doubleValue(), 0.01d );
	}

	@Test
	public void thatStaticFactoryMethodWithParametersIsAvailable() {
		assertEquals( 84L, injector.resolve( dependency( long.class ) ).longValue() );
	}

	/**
	 * The provider method
	 * {@link InspectorBindsImplementor1#instanceFactoryMethodWithParameters(float)} uses the
	 * {@link Resource} annotation to specify the name. As a result there is a named
	 * {@link Instance} that can be resolved.
	 */
	@Test
	public void thatNamedWithAnnotationCanBeUsedToGetNamedResources() {
		assertEquals( 42d,
				injector.resolve( dependency( double.class ).named( "foo" ) ).doubleValue(), 0.01d );
	}

	@Test
	public void thatMethodsAreBoundThatAreAssignableToSpecifiedType() {
		assertEquals(
				true,
				injector.resolve( dependency( Typecast.providerTypeOf( Boolean.class ) ) ).provide() );
	}

	@Test ( expected = NoSuchResourceException.class )
	public void thatNoMethodsAreBoundThatAreNotAssignableToSpecifiedType() {
		injector.resolve( dependency( Character.class ) );
	}

	@Test
	public void thatMethodsAreBoundThatAreInSpecifiedPackagesSet() {
		assertEquals( named( "foobar" ), injector.resolve( dependency( Name.class ) ) );
	}

	@Test ( expected = NoSuchResourceException.class )
	public void thatNoMethodsAreBoundThatAreNotInSpecifiedPackagesSet() {
		injector.resolve( dependency( String.class ) );
	}
}
