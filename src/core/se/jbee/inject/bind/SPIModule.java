package se.jbee.inject.bind;

import static se.jbee.inject.Type.raw;

import java.lang.reflect.Constructor;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.SPI;
import se.jbee.inject.Scope;
import se.jbee.inject.bootstrap.New;
import se.jbee.inject.bootstrap.Supply;
import se.jbee.inject.config.ConstructsBy;
import se.jbee.inject.container.Supplier;

/**
 * Provides a {@link Supplier} that can resolve all types extending an
 * {@link SPI} should they not be bound otherwise.
 * 
 * This is the basis of the {@link SPI} functionality where any type
 * implementing the abstraction is constructed by the {@link Supplier}.
 * 
 * The created instance is an effective singleton per type, so there will be one
 * instance for each {@link SPI} implementation class.
 * 
 * @since 19.1
 */
public class SPIModule extends BinderModule {

	@Override
	protected void declare() {
		ConstructsBy constructsBy = env(ConstructsBy.class);
		asDefault() //
				.per(Scope.dependencyType) //
				.bind(raw(SPI.class).asUpperBound()) //
				.toSupplier((dep, context) -> //
				extension(constructsBy, dep, context));
	}

	@SuppressWarnings("unchecked")
	private static <T> T extension(ConstructsBy constructsBy, Dependency<?> dep,
			Injector context) {
		Constructor<T> constructor = (Constructor<T>) constructsBy.reflect(
				dep.type().rawType);
		return Supply.constructor(New.bind(constructor)).supply(
				(Dependency<? super T>) dep, context);
	}
}
