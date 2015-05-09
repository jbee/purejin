package se.jbee.inject.bind;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static se.jbee.inject.bind.AssertInjects.assertEqualSets;

import java.io.Serializable;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.action.Action;
import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Module;

public class TestPluginBinds {

	private static class TestPluginModule extends BinderModule {
		
		@Override
		protected void declare() {
			asDefault().plug(TestExtensionService.class).into(Action.class);
			inPackageOf( Module.class ).plug(TestExtensionPackageLocalService.class).into(Action.class);
			injectingInto( Serializable.class ).plug(TestExtensionInstanceOfService.class).into(Action.class);
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

	private final Injector injector = Bootstrap.injector( TestPluginModule.class );

	@SuppressWarnings("rawtypes")
	private final Dependency<Class[]> dependency = Dependency.pluginsFor(Action.class);

	@Test
	public void thatJustUntargetedExtensionsAreResolvedGlobally() {
		Class<?>[] classes = injector.resolve( dependency );
		assertThat( classes.length, is( 1 ) );
		assertSame( TestExtensionService.class, classes[0] );
	}

	@Test
	public void thatPackageLocalExtensionsAreResolvedWithAppropiateInjection() {
		Class<?>[] classes = injector.resolve( dependency.injectingInto( Module.class ) );
		assertEqualSets( new Class<?>[] { TestExtensionService.class,
				TestExtensionPackageLocalService.class }, classes );
	}

	@Test
	public void thatInstanceOfExtensionsAreResolvedWithAppropiateInjection() {
		Class<?>[] classes = injector.resolve( dependency.injectingInto( String.class ) );
		assertEqualSets( new Class<?>[] { TestExtensionService.class,
				TestExtensionInstanceOfService.class }, classes );
	}
}
