/**
 * Contains an add-on for convenient type conversion based on the {@link
 * se.jbee.inject.Converter} abstraction.
 */
module se.jbee.inject.convert {

	requires transitive se.jbee.lang;
	requires transitive se.jbee.inject.api;
	requires transitive se.jbee.inject.bind;

	exports se.jbee.inject.convert;
}
