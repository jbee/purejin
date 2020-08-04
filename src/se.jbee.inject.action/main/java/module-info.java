module se.jbee.inject.action {

	requires se.jbee.inject.api;
	requires se.jbee.inject.bind;

	exports se.jbee.inject.action;

	opens se.jbee.inject.action to se.jbee.inject.api;
}
