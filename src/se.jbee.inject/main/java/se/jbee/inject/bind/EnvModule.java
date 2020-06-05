package se.jbee.inject.bind;

import se.jbee.inject.Scope;

public abstract class EnvModule extends BinderModule {

	@Override
	protected Bind init(Bind bind) {
		//TODO also set package in Target
		return bind.per(Scope.container);
	}
}
