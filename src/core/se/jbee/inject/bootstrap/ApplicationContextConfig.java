package se.jbee.inject.bootstrap;

import se.jbee.inject.config.Choices;
import se.jbee.inject.config.Edition;
import se.jbee.inject.config.Feature;
import se.jbee.inject.config.Globals;
import se.jbee.inject.config.Mirrors;
import se.jbee.inject.config.Options;

/**
 * This service interface is used to customise the bootstrapping of the
 * {@link Bootstrap#getApplicationContext()} via the
 * {@link java.util.ServiceLoader} mechanism.
 * 
 * As usual with {@link java.util.ServiceLoader} define a file in
 * <code>META-INF/services/se.jbee.inject.bootstrap.ApplicationContextConfig</code>
 * in any software module on the classpath containing the fully qualified class
 * name of the class implementing the application configuration.
 * 
 * @since 19.1
 */
public interface ApplicationContextConfig {

	/**
	 * {@link Options} are used to pass parameters to
	 * {@link se.jbee.inject.bind.BinderModuleWith}. Most often those come from
	 * configuration files or the like.
	 * 
	 * {@link Choices} are used to decide which of multiple-choice path in the
	 * tree of {@link Bundle}s are actually included. This is often used to
	 * customise the context to particular environments.
	 * 
	 * {@link Feature}s or {@link Edition}s are used to compose different
	 * versions of the same application based on e.g. a license key.
	 * 
	 * @return the {@link Globals}s used when bootstrapping
	 *         {@link Bootstrap#getApplicationContext()}
	 */
	default Globals globals() {
		return Globals.STANDARD;
	}

	/**
	 * {@link Mirrors}s are used to control the implicit selection of e.g.
	 * factory {@link java.lang.reflect.Method}s or
	 * {@link java.lang.reflect.Constructor}s or their parameter instances when
	 * using the binder API.
	 * 
	 * This can for example be used to guide binding and injection with user
	 * defined annotations or to alter the behaviour of the suppliers that
	 * create instances to perform special initialisation logic.
	 * 
	 * @return the {@link Mirrors} used when bootstrapping
	 *         {@link Bootstrap#getApplicationContext()}
	 */
	default Mirrors mirrors() {
		return Mirrors.DEFAULT;
	}

	/**
	 * {@link Macros} expand arguments passed to the fluent binder API into full
	 * bindings. Most importantly the provide the
	 * {@link se.jbee.inject.container.Supplier}s used can contain logic of
	 * further implicit or automatic binds. By customising {@link Macros} the
	 * default behaviour of the binder API can be changed.
	 * 
	 * @return the {@link Macros} used when bootstrapping
	 *         {@link Bootstrap#getApplicationContext()}
	 */
	default Macros macros() {
		return Macros.DEFAULT;
	}
}
