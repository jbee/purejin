/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * A {@link Supplier} is a source or factory for specific instances within the
 * given {@link Injector} context. It can be understood as the "backend"
 * abstraction that actually does the work within the internals of the {@link
 * Injector} implementation.
 * <p>
 * The {@link Supplier} declared in binding phase is later used in the form of
 * the {@link Provider} passed to the {@link Scope#provide(int, int, Dependency,
 * Provider)} method. Therefore using a {@link Supplier} creates instances
 * within a {@link Scope}.
 * <p>
 * This is in contrast to the {@link Generator} abstraction that is the
 * "frontend" or user facing abstraction to create or yield instances. Normally
 * a {@link Generator} is just a facade to reach down to the {@link Supplier}
 * backing it but it can also be an implementation that is not backed by a
 * {@link Supplier} which then means it also does not apply any {@link Scope}ing.
 *
 * @param <T> The type of the instance being resolved
 */
@FunctionalInterface
public interface Supplier<T> {

	/**
	 * This {@link Supplier} is asked to supply the instance that is used for
	 * the given {@link Dependency} (probably with help of the {@link
	 * Injector}).
	 */
	T supply(Dependency<? super T> dep, Injector context)
			throws UnresolvableDependency;

	/**
	 * Mostly defined to capture the contract by convention that when a {@link
	 * Supplier} class does implement {@link Generator} they are directly used
	 * as such and no generator is wrapped around the {@link Supplier} by the
	 * container.
	 *
	 * @return true, if this {@link Supplier} does support the {@link
	 * #asGenerator()} method and wants it to be used.
	 */
	default boolean isGenerator() {
		return this instanceof Generator;
	}

	/**
	 * Note that this method should only be called after making sure using
	 * {@link #isGenerator()} that this {@link Supplier} is indeed a {@link
	 * Generator} too.
	 *
	 * @return This {@link Supplier} as {@link Generator} as created by {@link
	 * #nonScopedBy(Generator)}.
	 * @see #isGenerator()
	 */
	@SuppressWarnings("unchecked")
	default Generator<T> asGenerator() {
		return (Generator<T>) this;
	}

	/**
	 * Wraps a {@link Generator} as {@link Supplier} so that scoping is bypassed
	 * and the {@link Generator} is asked directly to create instances.
	 *
	 * @param generator the generator used to directly create instances
	 * @param <T>       type of instance(s) created by the {@link Generator} and
	 *                  the {@link Supplier}
	 * @return The {@link Generator} wrapped into a {@link Supplier} in a way so
	 * it is unwrapped later on used {@link #asGenerator()}. This means no
	 * scoping effects occur as the provided {@link Generator} will directly be
	 * used each time an instance is needed.
	 */
	static <T> Supplier<T> nonScopedBy(Generator<T> generator) {
		/*
		 * This cannot be changed to a lambda since we need a type that actually
		 * implements both {@link Supplier} and {@link Generator}. This way the
		 * {@link Generator} is picked directly by the {@link Injector}.
		 */
		final class NonScopedGenerator<E>
				implements Supplier<E>, Generator<E> {

			private final Generator<E> generator;

			NonScopedGenerator(Generator<E> generator) {
				this.generator = generator;
			}

			@Override
			public E generate(Dependency<? super E> dep) {
				return generator.generate(dep);
			}

			@Override
			public E supply(Dependency<? super E> dep, Injector context) {
				return generate(dep);
			}

			@Override
			public String toString() {
				return "by "+ generator.toString();
			}
		}
		return new NonScopedGenerator<>(generator);
	}
}
