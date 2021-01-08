package se.jbee.inject.binder.spi;

import se.jbee.inject.Instance;
import se.jbee.inject.config.Config;

/**
 * Makes a configuration binding. Any configuration is local to a target {@link
 * Instance}.
 *
 * @since 8.1
 *
 * @param <B> return type of the binder step following a {@code configure} step
 */
@FunctionalInterface
public interface ConfiguringInstanceLocalBinder<B extends ParentLocalBinder<B>> extends
		InstanceLocalBinder<B> {

	/**
	 * Root for container "global" configuration.
	 *
	 * @since 8.1
	 */
	default B configure() {
		return injectingInto(Config.class);
	}

	/**
	 * Root for target type specific configuration.
	 *
	 * @since 8.1
	 */
	default B configure(Class<?> ns) {
		return configure().within(ns);
	}

	/**
	 * Root for {@link Instance} specific configuration.
	 *
	 * @since 8.1
	 */
	default B configure(Instance<?> ns) {
		return configure().within(ns);
	}
}
