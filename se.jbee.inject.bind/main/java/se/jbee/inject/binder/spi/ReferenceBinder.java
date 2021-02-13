package se.jbee.inject.binder.spi;

import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.lang.Type;

import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.raw;

/**
 * Bind a resource to a forward referenced {@link Instance}.
 * <p>
 * This is practically publishing the referenced {@link Instance} as the bound
 * "API" resource.
 *
 * @param <T> type of the bound "API" resource (minimum required type)
 */
@FunctionalInterface
public interface ReferenceBinder<T> {

	<I extends T> void to(Instance<I> instance);

	default <I extends T> void to(String name, Class<I> type) {
		to(named(name), type);
	}

	default <I extends T> void to(Name name, Class<I> type) {
		to(instance(name, raw(type)));
	}

	default <I extends T> void to(Name name, Type<I> type) {
		to(instance(name, type));
	}

	default <I extends T> void to(Class<I> impl) {
		to(Instance.anyOf(raw(impl)));
	}
}
