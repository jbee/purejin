package se.jbee.inject.binder.spi;

import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.lang.Type;

import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.raw;

/**
 * Make a binding local to the injected {@link Instance}.
 * <p>
 * Usually this is used to specify what parameter to use when injecting the
 * target instance.
 *
 * @since 8.1
 *
 * @param <B> return type of the binder step following a {@code injectingInto}
 *            step
 */
@FunctionalInterface
public interface InstanceLocalBinder<B> {

	/**
	 * Makes all bindings made with the returned binder local to the injection
	 * of the provided {@link Instance}, in other words when dependencies of
	 * that {@link Instance} are resolved.
	 *
	 * @param target the injected {@link Instance}
	 * @return next step in fluent API
	 */
	B injectingInto(Instance<?> target);

	/**
	 * @see #injectingInto(Instance)
	 */
	default B injectingInto(String name, Class<?> type) {
		return injectingInto(named(name), raw(type));
	}

	/**
	 * @see #injectingInto(Instance)
	 */
	default B injectingInto(Name name, Type<?> type) {
		return injectingInto(Instance.instance(name, type));
	}

	/**
	 * @see #injectingInto(Instance)
	 */
	default B injectingInto(Type<?> target) {
		return injectingInto(defaultInstanceOf(target));
	}

	/**
	 * @see #injectingInto(Instance)
	 */
	default B injectingInto(Class<?> target) {
		return injectingInto(raw(target));
	}

}
