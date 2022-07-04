/**
 * Contains the defaults for {@link se.jbee.inject.Scope} and {@link
 * se.jbee.inject.bind.ValueBinder}s required to {@link
 * se.jbee.inject.bootstrap.Bootstrap} a {@link se.jbee.inject.Injector} from
 * {@link se.jbee.inject.bind.Binding}s.
 * <p>
 * In multi-module application this module should only be required in the single
 * module responsible for bootstrapping the application.
 */
module se.jbee.inject.bootstrap {

	requires java.logging;

	requires transitive se.jbee.lang;
	requires transitive se.jbee.inject.api;
	requires transitive se.jbee.inject.bind;
	requires se.jbee.inject.container;

	exports se.jbee.inject.bootstrap;
	exports se.jbee.inject.defaults;
	exports se.jbee.inject.scope;

}
