package se.jbee.inject.binder.spi;

import se.jbee.inject.Env;
import se.jbee.inject.Name;
import se.jbee.inject.config.Connector;
import se.jbee.inject.config.ProducesBy;

import static se.jbee.inject.Name.named;

@FunctionalInterface
public interface ConnectorBinder {

	/**
	 * Name of the {@link Connector} used for action feature.
	 */
	String ACTION_CONNECTOR = "actions";

	String EVENT_CONNECTOR = "on";

	/**
	 * Name of the {@link Connector} used for the scheduler feature.
	 */
	String SCHEDULER_CONNECTOR = "scheduler";

	/**
	 * The qualifier {@link Name} used in the {@link Env} for a special {@link
	 * ProducesBy} used in connection with {@link Connector}. If no special
	 * {@link ProducesBy} is set the default named one is used.
	 */
	String CONNECT_QUALIFIER = "connect";

	void to(Name connectorName);

	default void to(String connectorName) {
		to(named(connectorName));
	}

	default void to(Class<?> connectorName) {
		to(named(connectorName));
	}

	default void asAction() {
		to(ACTION_CONNECTOR);
	}

	default void asEvent() {
		to(EVENT_CONNECTOR);
	}

	default void asScheduled(String type) {
		asScheduled(named(type));
	}
	default void asScheduled(Name type) {
		to(type.in(SCHEDULER_CONNECTOR));
	}
}
