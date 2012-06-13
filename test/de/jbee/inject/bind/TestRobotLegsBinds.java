package de.jbee.inject.bind;

import org.junit.Test;

import de.jbee.inject.bind.PackageModule;

public class TestRobotLegsBinds {

	public static class RobotLegsModule
			extends PackageModule {

		@Override
		protected void configure() {
			// TODO Auto-generated method stub

		}
	}

	static class Foot {

	}

	static class LeftFoot
			extends Foot {

	}

	static class RightFoot
			extends Foot {

	}

	@Test
	public void test() {

	}
}
