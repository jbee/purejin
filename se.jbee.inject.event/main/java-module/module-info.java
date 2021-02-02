/**
 * Contains an add-on for an <strong>experimental</strong> asynchronous event
 * dispatch on vanilla java interfaces and methods.
 */
module se.jbee.inject.event {

	requires se.jbee.inject.lang;
	requires se.jbee.inject.api;
	requires se.jbee.inject.bind;
	requires se.jbee.inject.bootstrap;

	exports se.jbee.inject.event;
	exports se.jbee.inject.schedule;
	exports se.jbee.inject.disk;

}
