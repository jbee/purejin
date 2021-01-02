package se.jbee.inject;

/**
 * {@link ContextAware} is a callback or hook invoked each time an instance of
 * the implementing class is injected or resolved allowing the implementation
 * class to return a modified instance that is adopted to the context.
 *
 * While this could be used to manipulate the instance state in place it is very
 * unlikely that this is correct as injections in other places and threads would
 * cause such in place modifications. Instead implementations should return
 * instances with modified state that reflects the adaptation to the
 * {@link Dependency} context.
 *
 * In contrast to an {@link Lift} the {@link ContextAware} is not called
 * on construction but each time the instance is injected (resolved) by the
 * {@link Injector}. This is limited to scoped {@link Resource}s. This are
 * {@link Resource}s that neither are in {@link Scope}
 * {@link Scope#container} or {@link Scope#reference} and which do not directly
 * generate their instances by having the {@link Supplier} also implement
 * {@link Generator}. Such {@link Resource}s are not
 * {@link ContextAware} by default.
 *
 * @since 8.1
 *
 * @param <T> type of the instance being resolved which itself is an instance of
 *            {@link ContextAware}. This should be the same type as the class
 *            implementing the {@link ContextAware} interface. This is a
 *            self-referential type construct similar to {@link Enum}.
 */
@FunctionalInterface
public interface ContextAware<T> {

	/**
	 * Returns this instance adopted to the provided context.
	 *
	 * @param context the {@link Dependency} currently resolved/injected which
	 *            is the context to adapt to
	 * @return This instance adopted to the context
	 */
	T inContext(Dependency<? super T> context);
}
