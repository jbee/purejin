package se.jbee.inject.binder.spi;

import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.lang.Type;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.raw;

/**
 * Make a binding local to the parent {@link Instance} of the currently injected
 * bean.
 *
 * Multiple calls to {@link #within(Instance)} (and its variants) can be used
 * to make the binding to a parent hierarchy.
 *
 * @since 8.1
 *
 * @param <B> return type of the binder step following a {@code within} step
 */
@FunctionalInterface
public interface ParentLocalBinder<B> {

	/**
	 * Makes all bindings made with the returned binder local to the provided
	 * parent instance.
	 *
	 * @param parent the parent {@link Instance} to match
	 * @return next step in fluent API
	 */
	B within(Instance<?> parent);

	/**
	 * @see #within(Instance)
	 */
	default B within(String name, Class<?> parent) {
		return within(instance(named(name), raw(parent)));
	}

	/**
	 * @see #within(Instance)
	 */
	default B within(Name name, Type<?> parent) {
		return within(instance(name, parent));
	}

	/**
	 * Makes all bindings made with the returned binder local to any parent
	 * having the provided type.
	 *
	 * @param parent the type of parent instances to match
	 * @return next step in fluent API
	 */
	default B within(Class<?> parent) {
		return within(raw(parent));
	}

	/**
	 * Makes all bindings made with the returned binder local to any parent
	 * having the provided type.
	 *
	 * @param parent the type of parent instances to match
	 * @return next step in fluent API
	 */
	default B within(Type<?> parent) {
		return within(anyOf(parent));
	}
}
