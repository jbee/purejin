/**
 * Contains an add-on to share and call vanilla Java methods as {@link
 * se.jbee.inject.action.Action}s to decouple caller and called code.
 */
module se.jbee.inject.action {

	requires transitive se.jbee.lang;
	requires transitive se.jbee.inject.api;
	requires transitive se.jbee.inject.bind;

	exports se.jbee.inject.action;
}
