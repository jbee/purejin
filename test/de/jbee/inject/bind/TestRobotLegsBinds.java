package de.jbee.inject.bind;

import org.junit.Test;

import de.jbee.inject.bind.BinderModule;

public class TestRobotLegsBinds {

	public static class RobotLegsModule
			extends BinderModule {

		@Override
		protected void declare() {
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
