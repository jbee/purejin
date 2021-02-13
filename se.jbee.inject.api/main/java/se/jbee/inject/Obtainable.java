package se.jbee.inject;

import se.jbee.lang.Type;

import java.util.function.Function;
import java.util.function.Supplier;

import static se.jbee.lang.Type.raw;

/**
 * A {@link java.util.Optional} like type that is "wrapped" around the type that
 * originally should have been injected to prevent failure to create the
 * receiving instance because one of its dependencies wasn't available.
 * <p>
 * Like an {@link Injector} a {@link Obtainable} has special semantics for
 * target types of 1-dimensional arrays. These are obtained element by element.
 * Any element that cannot be obtained is "cutout" in the returned array. This
 * means only the elements that could be resolved are contained in the
 * obtainable array. In any case an array will be returned even if it should be
 * empty.
 * <p>
 * In a way the {@link Obtainable} is a marker for the user to allow failure
 * to resolve dependencies.
 * <p>
 * The special behaviour is the intended use of an {@link Obtainable}. Its role
 * is to compose an application in a way that allows dependencies of a
 * "plug-ins" nature to be obtain if they are valid and thus can be resolved
 * successfully. Otherwise this "plug-in" nature instances are simply excluded
 * from the application by not being included in the returned array.
 * <p>
 * This mechanism is also put in place for {@link java.util.List}, {@link
 * java.util.Set} and {@link java.util.Collection} target types.
 *
 * @param <T> The type of the instance that should be obtained
 *
 * @since 8.1
 */
@FunctionalInterface
public interface Obtainable<T> {

	static <T> Type<Obtainable<T>> obtainableTypeOf(Class<T> obtained) {
		return obtainableTypeOf(raw(obtained));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <T> Type<Obtainable<T>> obtainableTypeOf(Type<T> obtained) {
		return (Type) raw(Obtainable.class).parameterized(obtained);
	}

	/**
	 * Resolves the instance of the obtained type or {@code null}  (non array
	 * and collection types) or empty array/collection (collection types) if
	 * that fails.
	 *
	 * @return the value if available, or {@code null} when not, for
	 * 1-dimensional array types all elements that could be resolved are
	 * returned in an array (which could be empty)
	 */
	T obtain();

	default boolean isAvailable() {
		return obtain() != null;
	}

	/**
	 * Returns the obtained value or a provided default value
	 *
	 * @param defaultValue the value to return when the bound value could not be
	 *                     obtained
	 * @return the obtained value (non {code null}) or the provided default.
	 * This only returns {@code null} in case it was provided as default value
	 * and the default value is returned
	 */
	default T orElse(T defaultValue) {
		T res = obtain();
		return res == null ? defaultValue : res;
	}

	/**
	 * Returns the obtained value or throws the provided exception.
	 *
	 * @param exceptionSupplier supplies the thrown exception in case the value
	 *                          cannot be obtained.
	 * @return the obtained value (if successful)
	 * @throws X In case the value can not be obtained the supplied exception is
	 *           thrown
	 */
	default <X extends Exception> T orElseThrow(
			Supplier<? extends X> exceptionSupplier) throws X {
		T res = obtain();
		if (res != null)
			return res;
		throw exceptionSupplier.get();
	}

	/**
	 * Returns the obtained value or throws the original exception transformed.
	 *
	 * @param exceptionTransformer function that wraps, unwraps, replaces or
	 *                             keeps the original exception to the exception
	 *                             thrown in case the value cannot be obtained
	 * @return the obtained value (if successful)
	 * @throws X In case the value can not be obtained the original exception
	 *           transformed to X is thrown
	 */
	default <X extends Exception> T orElseThrow(
			Function<UnresolvableDependency, ? extends X> exceptionTransformer)
			throws X {
		T res = obtain();
		if (res != null)
			return res;
		throw exceptionTransformer.apply(null);
	}

	/**
	 * @see #orElseThrow(Function)
	 */
	default T orElseThrow() throws UnresolvableDependency {
		return orElseThrow(e -> e);
	}
}
