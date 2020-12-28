package se.jbee.inject.binder;

import se.jbee.inject.bind.Bundle;

import java.util.ServiceLoader;

public abstract class FilteredServiceLoaderBundles extends BootstrapperBundle {

	@Override
	protected final void bootstrap() {
		for (Bundle bundle : ServiceLoader.load(Bundle.class)) {
			Class<? extends Bundle> bundleId = bundle.getClass();
			if (bootstrap(bundleId)) {
				install(bundleId);
			}
		}
	}

	/**
	 * In the instance of {@link Bundle}s composition isn't as meaningful as in
	 * most other contexts as {@link Bundle} are purely identified by {@link
	 * Class}. This means two instances of the same {@link Bundle}
	 * implementation with different inner state make no sense. Therefore we do
	 * use inheritance over composition.
	 *
	 * @param bundle a {@link Bundle} provided by {@link ServiceLoader}
	 * @return true, if the bundle should be installed, else false
	 */
	abstract boolean bootstrap(Class<? extends Bundle> bundle);
}
