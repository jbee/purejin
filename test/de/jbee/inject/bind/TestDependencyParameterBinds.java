package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.util.logging.Logger;

import org.junit.Test;

import de.jbee.inject.Dependency;
import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Instance;
import de.jbee.inject.Parameter;

/**
 * This test demonstrates the most powerful {@link Parameter} hint: a {@link Dependency}.
 * 
 * It allows to also describe what {@link Instance} should be used dependent on its parent(s) it
 * would be {@link Dependency#injectingInto(Class)}. Though this we can tell to inject the
 * {@link Logger} that would be injected into the {@link BinderModule} class into our test object
 * {@link LoggerInspector}.
 * 
 * @see TestConstructorParameterBinds
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public class TestDependencyParameterBinds {

	private static class DependencyParameterBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( BuildinBundle.LOGGER );
			install( DependencyParameterBindsModule.class );
		}

	}

	private static class DependencyParameterBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( LoggerInspector.class ).toConstructor(
					dependency( Logger.class ).injectingInto( BinderModule.class ) );
		}

	}

	private static class LoggerInspector {

		final Logger logger;

		@SuppressWarnings ( "unused" )
		LoggerInspector( Logger logger ) {
			this.logger = logger;
		}
	}

	@Test
	public void thatDependencyParameterIsUnderstood() {
		DependencyResolver resolver = Bootstrap.injector( DependencyParameterBindsBundle.class );
		LoggerInspector inspector = resolver.resolve( dependency( LoggerInspector.class ) );
		assertThat( inspector.logger,
				sameInstance( Logger.getLogger( BinderModule.class.getCanonicalName() ) ) );
	}
}
