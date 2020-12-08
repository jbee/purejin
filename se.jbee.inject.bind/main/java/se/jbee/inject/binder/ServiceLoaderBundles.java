package se.jbee.inject.binder;

import se.jbee.inject.Extends;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.Bundle;

/**
 * A {@link Bundle} that installs all {@link Bundle}s provided via {@link
 * java.util.ServiceLoader} which are not annotated with {@link Extends} or with
 * are annotated with {@link Extends} referring to {@link Injector} type.
 *
 * @since 8.1
 */
public class ServiceLoaderBundles extends FilteredServiceLoaderBundles {

	@Override
	boolean bootstrap(Class<? extends Bundle> bundle) {
		if (!bundle.isAnnotationPresent(Extends.class))
			return true;
		Class<?> target = bundle.getAnnotation(Extends.class).value();
		return target == Injector.class;
	}
}
