module se.jbee.junit.assertion {

	requires org.junit.jupiter.api;
	requires org.junit.platform.engine;
	requires org.junit.platform.launcher;

	exports se.jbee.junit.assertion;

	opens se.jbee.junit.assertion to org.junit.platform.commons;

	provides org.junit.platform.launcher.TestExecutionListener with
			se.jbee.junit.assertion.listener.ContainerFeed;
}
