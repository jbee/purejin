package test.example1;

import se.jbee.inject.binder.BinderModuleWith;

public class Example1Module extends BinderModuleWith<Integer> {

	@Override
	protected void declare(Integer value) {
		// the value is defined in the Env, in this case it is from
		// MyEnvSetupBundle which sets it to 13
		bind(int.class).to(value);
		bind(String.class).to(getClass().getName());
	}

}
