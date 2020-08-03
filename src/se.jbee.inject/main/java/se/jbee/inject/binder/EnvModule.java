package se.jbee.inject.binder;

import se.jbee.inject.Scope;
import se.jbee.inject.bind.Bind;

public abstract class EnvModule extends BinderModule {

	@Override
	protected Bind init(Bind bind) {
		//TODO also set package in Target
		return bind.per(Scope.container);
	}
}
