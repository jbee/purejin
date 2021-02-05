module se.jbee.junit.assertion {

	requires org.junit.jupiter;
	requires org.junit.vintage.engine;

	exports se.jbee.junit.assertion;

	opens se.jbee.junit.assertion to org.junit.platform.commons;
}
