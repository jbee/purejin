package se.jbee.inject.binder;

import se.jbee.inject.Env;
import se.jbee.inject.Extends;
import se.jbee.inject.bind.Bundle;

/**
 * A {@link Bundle} that installs all {@link Bundle}s declared via {@link
 * java.util.ServiceLoader} which also got annotated with {@link Extends}
 * referring to {@link Env}.
 *
 * @since 8.1
 */
public class ServiceLoaderEnvBundles extends FilteredServiceLoaderBundles {

	@Override
	boolean bootstrap(Class<? extends Bundle> bundle) {
		if (!bundle.isAnnotationPresent(Extends.class))
			return false;
		Class<?> target = bundle.getAnnotation(Extends.class).value();
		return target == Env.class;
	}

}
