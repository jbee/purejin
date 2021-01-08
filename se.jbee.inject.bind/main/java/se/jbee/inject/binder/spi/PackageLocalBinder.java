package se.jbee.inject.binder.spi;

import se.jbee.inject.Packages;

import static se.jbee.inject.Packages.*;

/**
 * Make a binding local to a specific package. This means the binding only
 * applies for an injection if the injected bean class is defined within the
 * packages declared.
 *
 * @since 8.1
 *
 * @param <B> return type of the binder step following a {@code in} step
 */
@FunctionalInterface
public interface PackageLocalBinder<B> {

	/**
	 * Make all bindings made with the returned binder local to the provided set
	 * of {@link Packages}.
	 *
	 * @param packages a set of {@link Packages} all following binding should
	 *                 apply to
	 * @return next step in fluent API
	 */
	B in(Packages packages);

	/**
	 * @see #in(Packages)
	 */
	default B inPackageAndSubPackagesOf(Class<?> type) {
		return in(packageAndSubPackagesOf(type));
	}

	/**
	 * @see #in(Packages)
	 */
	default B inPackageOf(Class<?> type) {
		return in(packageOf(type));
	}

	/**
	 * @see #in(Packages)
	 */
	default B inSubPackagesOf(Class<?> type) {
		return in(subPackagesOf(type));
	}

}
