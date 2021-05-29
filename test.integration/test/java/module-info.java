open module test.integration {

	requires java.logging;
	requires org.junit.jupiter;
	requires static org.junit.platform.console; // <- launches test modules
	requires static org.junit.platform.jfr; // <- flight-recording support

	requires se.jbee.junit.assertion;

	/* core */
	requires transitive se.jbee.inject;
	/* and the add-ons */
	requires se.jbee.inject.convert;
	requires se.jbee.inject.action;
	requires se.jbee.inject.event;
	requires se.jbee.inject.contract;

	requires test.examples; // <- module under test
}
