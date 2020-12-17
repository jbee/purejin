package se.jbee.inject.defaults;

import static se.jbee.inject.binder.New.newInstance;
import static se.jbee.inject.lang.Type.raw;

import java.lang.reflect.Constructor;

import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Supply;
import se.jbee.inject.config.ConstructsBy;
import se.jbee.inject.config.Extension;

/**
 * Provides a {@link Supplier} that can resolve all types extending an
 * {@link Extension} should they not be bound otherwise.
 *
 * This is the basis of the {@link Extension} functionality where any type
 * implementing the abstraction is constructed by that {@link Supplier}.
 *
 * The created instance is an effective singleton per type, so there will be one
 * instance for each {@link Extension} implementation class.
 *
 * @since 8.1
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
		Constructor<T> ext = (Constructor<T>) constructsBy.reflect(
				dep.type().rawType.getDeclaredConstructors());
		context.resolve(Env.class).accessible(ext);
		return Supply.byNew(newInstance(ext)) //
				.supply((Dependency<? super T>) dep, context);
	}
}
