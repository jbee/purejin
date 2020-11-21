open module test.integration {

	requires java.logging;
	requires org.junit.jupiter;

	/* core */
	requires transitive se.jbee.inject;
	/* and the add-ons */
	requires se.jbee.inject.convert;
	requires se.jbee.inject.action;
	requires se.jbee.inject.event;

	requires com.example.app; // <- module under test
}
