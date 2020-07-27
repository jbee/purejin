package se.jbee.inject.bind;

import static se.jbee.inject.Type.raw;

import java.lang.reflect.Constructor;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.bootstrap.New;
import se.jbee.inject.bootstrap.Supply;
import se.jbee.inject.config.ConstructsBy;
import se.jbee.inject.container.Supplier;
import se.jbee.inject.extend.Extension;

/**
 * Provides a {@link Supplier} that can resolve all types extending an
 * {@link Extension} should they not be bound otherwise.
 * 
 * This is the basis of the {@link Extension} functionality where any type
 * implementing the abstraction is constructed by the {@link Supplier}.
 * 
 * The created instance is an effective singleton per type, so there will be one
 * instance for each {@link Extension} implementation class.
 * 
 * @since 19.1
 */
// intentionally made default visible to not be confused with a module that is useful as a base class for user modules
class ExtensionModule extends BinderModule {

	@Override
	protected void declare() {
		ConstructsBy constructsBy = env(ConstructsBy.class);
		asDefault() //
				.per(Scope.dependencyType) //
				.bind(raw(Extension.class).asUpperBound()) //
				.toSupplier((dep, context) -> //
				extension(constructsBy, dep, context));
	}

	@SuppressWarnings("unchecked")
	private static <T> T extension(ConstructsBy constructsBy, Dependency<?> dep,
			Injector context) {
		Constructor<T> constructor = (Constructor<T>) constructsBy.reflect(
				dep.type().rawType);
		return Supply.byNew(New.bind(constructor)).supply(
				(Dependency<? super T>) dep, context);
	}
}
