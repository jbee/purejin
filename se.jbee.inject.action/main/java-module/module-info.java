/**
 * Contains an add-on to share and call vanilla Java methods as {@link
 * se.jbee.inject.action.Action}s to decouple caller and called code.
 */
module se.jbee.inject.action {

	requires se.jbee.inject.lang;
	requires se.jbee.inject.api;
	requires se.jbee.inject.bind;

	exports se.jbee.inject.action;
}
