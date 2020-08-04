open module test.integration {

	requires java.logging;
	requires junit;

	/* core */
	requires transitive se.jbee.inject;
	/* and the add-ons */
	requires se.jbee.inject.convert;
	requires se.jbee.inject.action;
	requires se.jbee.inject.event;

}
