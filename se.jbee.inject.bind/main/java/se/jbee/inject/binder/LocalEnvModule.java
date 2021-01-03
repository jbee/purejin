package se.jbee.inject.binder;

import se.jbee.inject.bind.Bind;

/**
 * A {@link EnvModule} where all made binds (by default) only apply to the
 * package of the extending class and all its sub-packages.
 *
 * This merely exists for clarity and convenience and as a way to learn how
 * to create a package localised effect.
 */
public abstract class LocalEnvModule extends EnvModule {

	@Override
	protected Bind init(Bind bind) {
		Bind res = super.init(bind);
		return res.with(res.target.inPackageAndSubPackagesOf(getClass()));
	}
}
