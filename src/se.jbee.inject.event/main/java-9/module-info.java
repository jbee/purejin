module se.jbee.inject.event {

	requires se.jbee.inject.api;
	requires se.jbee.inject.bind;

	exports se.jbee.inject.event;

	opens se.jbee.inject.event to se.jbee.inject.api;
}
