package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static se.jbee.inject.bind.AssertInjects.assertEqualSets;

import java.io.Serializable;
import java.util.concurrent.Callable;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.action.Action;
import se.jbee.inject.bind.Binder;
import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.bind.Binder.PluginBinder;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Module;

/**
 * Plug-in-binds are a convenient way to define a named set of classes. The name
 * of the set is given by the class stated in the
 * {@link PluginBinder#into(Class)} method. The {@link Binder#plug(Class)}
 * states the class that should be added to a set.
 * 
 * The set than can be received from the {@link Injector} using
 * {@link Dependency#pluginsFor(Class)}. This allows to easily build
 * abstractions like actions that use this to collect the set of classes
 * used to look for action methods.
 * 
 * The sets are just ordinary bindings on {@link Class}. That means the sets can
 * contain different members depending on the actual {@link Dependency} if the
 * scope of the bind done is narrowed like in this example.
 */
public class TestPluginBinds {

	private static class TestPluginModule extends BinderModule {
		
		@Override
		protected void declare() {
			asDefault().plug(TestExtensionAction.class).into(Callable.class);
			inPackageOf( Module.class ).plug(TestExtensionPackageLocalAction.class).into(Callable.class);
			injectingInto( Serializable.class ).plug(TestExtensionInstanceOfAction.class).into(Callable.class);
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
	private final Dependency<Class[]> dependency = Dependency.pluginsFor(Callable.class);

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
