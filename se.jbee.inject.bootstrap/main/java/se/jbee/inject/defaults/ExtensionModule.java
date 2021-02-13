package se.jbee.inject.defaults;

import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Supply;
import se.jbee.inject.config.ConstructsBy;
import se.jbee.inject.config.Extension;
import se.jbee.lang.Type;

import java.lang.reflect.Constructor;

import static se.jbee.inject.binder.Constructs.constructs;
import static se.jbee.lang.Type.raw;

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
		asDefault() //
				.per(Scope.dependencyType) //
				.bind(raw(Extension.class).asUpperBound()) //
				.toSupplier(ExtensionModule::extension);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <T> T extension(Dependency<?> dep, Injector context) {
		Type<?> expectedType = dep.type();
		Env env = context.resolve(Env.class).in(expectedType.rawType);
		ConstructsBy constructsBy = env.property(ConstructsBy.class);
		Constructor<?> ext = constructsBy.reflect(
				expectedType.rawType.getDeclaredConstructors());
		return (T) Supply.byConstruction(constructs(expectedType, ext, env)) //
				.supply((Dependency) dep, context);
	}
}
