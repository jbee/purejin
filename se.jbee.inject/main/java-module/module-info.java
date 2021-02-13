/**
 * A BOM type module that references the core modules of the library
 */
@SuppressWarnings("Java9RedundantRequiresStatement")
module se.jbee.inject {

	requires transitive se.jbee.lang;
	requires transitive se.jbee.inject.api;
	requires transitive se.jbee.inject.bind;
	requires transitive se.jbee.inject.container;
	requires transitive se.jbee.inject.bootstrap;
}
