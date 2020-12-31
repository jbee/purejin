package se.jbee.inject.binder;

import se.jbee.inject.bind.Bind;

public abstract class LocalEnvModule extends EnvModule {

	@Override
	protected Bind init(Bind bind) {
		Bind res = super.init(bind);
		return res.with(res.target.inPackageAndSubPackagesOf(getClass()));
	}
}
