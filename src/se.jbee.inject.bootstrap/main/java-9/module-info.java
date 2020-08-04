module se.jbee.inject.bootstrap {

	requires java.logging;
	requires se.jbee.inject.api;
	requires se.jbee.inject.bind;
	requires se.jbee.inject.container;

	exports se.jbee.inject.bootstrap;
	exports se.jbee.inject.defaults;
	exports se.jbee.inject.scope;

	opens se.jbee.inject.defaults to se.jbee.inject.api;
}
