package de.jbee.inject.bind;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import de.jbee.inject.Dependency;
import de.jbee.inject.Injector;
import de.jbee.inject.service.ServiceMethod.ServiceClassExtension;

public class TestExtensionBinds {

	private static class TestExtensionModule
			extends BinderModule {

		@Override
		protected void declare() {
			extend( ServiceClassExtension.class, TestExtensionService.class );
			inPackageOf( Module.class ).extend( ServiceClassExtension.class,
					TestExtensionPackageLocalService.class );
			injectingInto( Serializable.class ).extend( ServiceClassExtension.class,
					TestExtensionInstanceOfService.class );
		}
	}

	private static class TestExtensionService {
		// just to see that it is resolved as service class
	}

	private static class TestExtensionPackageLocalService {
		// just to see that it is resolved as service class
	}

	private static class TestExtensionInstanceOfService {
		// just to see that it is resolved as service class
	}

	private final Injector injector = Bootstrap.injector( TestExtensionModule.class );
	private final Dependency<Class[]> dependency = Extend.dependency( ServiceClassExtension.class );

	@Test
	public void thatJustUntargetedExtensionsAreResolvedGlobally() {
		Class<?>[] classes = injector.resolve( dependency );
		assertThat( classes.length, is( 1 ) );
		assertSame( classes[0], TestExtensionService.class );
	}

	@Test
	public void thatPackageLocalExtensionsAreResolvedWithAppropiateInjection() {
		Class<?>[] classes = injector.resolve( dependency.injectingInto( Module.class ) );
		assertSameElements( classes, new Class<?>[] { TestExtensionService.class,
				TestExtensionPackageLocalService.class } );
	}

	@Test
	public void thatInstanceOfExtensionsAreResolvedWithAppropiateInjection() {
		Class<?>[] classes = injector.resolve( dependency.injectingInto( String.class ) );
		assertSameElements( classes, new Class<?>[] { TestExtensionService.class,
				TestExtensionInstanceOfService.class } );
	}

	private <T> void assertSameElements( T[] expected, T[] actual ) {
		assertEquals( expected.length, actual.length );
		assertEquals( new HashSet<T>( Arrays.asList( expected ) ), new HashSet<T>(
				Arrays.asList( actual ) ) );
	}
}
