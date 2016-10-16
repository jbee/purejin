package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static se.jbee.inject.bind.AssertInjects.assertEqualSets;

import java.io.Serializable;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.action.Action;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Module;

public class TestPluginBinds {

	private static class TestPluginModule extends BinderModule {
		
		@Override
		protected void declare() {
			asDefault().plug(TestExtensionAction.class).into(Action.class);
			inPackageOf( Module.class ).plug(TestExtensionPackageLocalAction.class).into(Action.class);
			injectingInto( Serializable.class ).plug(TestExtensionInstanceOfAction.class).into(Action.class);
		}
	}

	private static class TestExtensionAction {
		// just to see that it is resolved as action class
	}

	private static class TestExtensionPackageLocalAction {
		// just to see that it is resolved as action class
	}

	private static class TestExtensionInstanceOfAction {
		// just to see that it is resolved as action class
	}

	private final Injector injector = Bootstrap.injector( TestPluginModule.class );

	@SuppressWarnings("rawtypes")
	private final Dependency<Class[]> dependency = Dependency.pluginsFor(Action.class);

	@Test
	public void thatJustUntargetedExtensionsAreResolvedGlobally() {
		Class<?>[] classes = injector.resolve( dependency );
		assertEquals( 1, classes.length );
		assertSame( TestExtensionAction.class, classes[0] );
	}

	@Test
	public void thatPackageLocalExtensionsAreResolvedWithAppropiateInjection() {
		Class<?>[] classes = injector.resolve( dependency.injectingInto( Module.class ) );
		assertEqualSets( new Class<?>[] { TestExtensionAction.class,
				TestExtensionPackageLocalAction.class }, classes );
	}

	@Test
	public void thatInstanceOfExtensionsAreResolvedWithAppropiateInjection() {
		Class<?>[] classes = injector.resolve( dependency.injectingInto( String.class ) );
		assertEqualSets( new Class<?>[] { TestExtensionAction.class,
				TestExtensionInstanceOfAction.class }, classes );
	}
}
