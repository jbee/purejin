package se.jbee.inject.binder.spi;

import se.jbee.inject.*;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Binds a resource directly to a provided {@link Supplier}.
 *
 * @param <T> type of the bound resource
 */
@FunctionalInterface
public interface SupplierBinder<T> {

	void toSupplier(Supplier<? extends T> supplier);

	/**
	 * Utility method to bind to a {@link Supplier} implementation that needs to
	 * be created within the {@link Injector} with help of the {@link
	 * Injector}.
	 *
	 * @param factory function that provided the {@link Injector} creates the
	 *                {@link Supplier} to use
	 * @param <S>     type if the created and used {@link Supplier}
	 * @since 8.1
	 */
	default <S extends Supplier<? extends T>> void toSupplier(
			Function<Injector, S> factory) {
		AtomicReference<S> cache = new AtomicReference<>();
		toSupplier((dep, context) ->
				cache.updateAndGet(e -> e != null ? e :
						factory.apply(context)).supply(dep, context));
	}

	/**
	 * Utility method that creates the instances from the {@link Injector}
	 * context given.
	 *
	 * This is used when a full {@link Supplier} contract is not needed to
	 * save stating the not needed {@link Dependency} argument.
	 *
	 * @since 8.1
	 */
	default void toFactory(Function<Injector, T> factory) {
		toSupplier((dep, context) -> factory.apply(context));
	}

	/**
	 * This method will bind the provided {@link Generator} in a way that
	 * bypasses {@link Scope} effects. The provided {@link Generator} is
	 * directly called to generate the instance each time it should be
	 * injected.
	 * <p>
	 * If a {@link Scope} should apply use {@link #toSupplier(Supplier)}
	 * instead or create a {@link Generator} bridge that does not implement
	 * {@link Generator} itself.
	 *
	 * @param generator used to create instances directly (with no {@link
	 *                  Scope} around it)
	 * @since 8.1
	 */
	default void toGenerator(Generator<? extends T> generator) {
		toSupplier(Supplier.nonScopedBy(generator));
	}

	/**
	 * In contrast to {@link #toGenerator(Generator)} this is just an
	 * ordinary adapter between {@link Generator} and {@link Supplier}. The
	 * provided {@link Generator} becomes usable as {@link Supplier}
	 * internally with all normal {@link Scope}ing effects occurring.
	 *
	 * @param generator used to create instances with a {@link Scope}
	 * @since 8.1
	 */
	default void toScopedGenerator(Generator<? extends  T> generator) {
		toSupplier((dep, context) -> generator.generate(dep));
	}

	/**
	 * @since 8.1
	 */
	default void toProvider(java.util.function.Supplier<? extends T> method) {
		toSupplier((dep, context) -> method.get());
	}
}
