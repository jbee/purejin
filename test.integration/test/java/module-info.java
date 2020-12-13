open module test.integration {

	requires java.logging;
	requires org.junit.jupiter;
	requires static org.junit.platform.console; // <- launches test modules

	/* core */
	requires transitive se.jbee.inject;
	/* and the add-ons */
	requires se.jbee.inject.convert;
	requires se.jbee.inject.action;
	requires se.jbee.inject.event;

	requires test.example.app; // <- module under test
}
