/**
 * Contains the fluent {@link se.jbee.inject.binder.Binder} API used by software
 * modules to issue their bindings in a convenient way.
 * <p>
 * In multi-module application this is usually required by each module to bind
 * the services provided by the software module.
 */
module se.jbee.inject.bind {

	requires transitive se.jbee.lang;
	requires transitive se.jbee.inject.api;

	exports se.jbee.inject.bind;
	exports se.jbee.inject.binder;
	exports se.jbee.inject.binder.spi;
	exports se.jbee.inject.config;

	uses se.jbee.inject.bind.Bundle;
	uses se.jbee.inject.bind.ModuleWith;
}
