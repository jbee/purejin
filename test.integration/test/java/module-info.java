open module test.integration {

	requires java.logging;
	requires org.junit.jupiter;
	requires org.junit.vintage.engine;

	// automatic modules (needed to use awaitility):
	requires junit;
	requires org.hamcrest;
	requires awaitility.test.support;
	requires awaitility;

	requires static org.junit.platform.console; // <- launches test modules


	/* core */
	requires transitive se.jbee.inject;
	/* and the add-ons */
	requires se.jbee.inject.convert;
	requires se.jbee.inject.action;
	requires se.jbee.inject.event;
	requires se.jbee.inject.contract;

	requires test.examples; // <- module under test
}
