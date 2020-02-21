package se.jbee.inject.bind;

import static se.jbee.inject.Type.raw;

import java.lang.reflect.Constructor;

import se.jbee.inject.Dependency;
import se.jbee.inject.Extension;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.bootstrap.Module;
import se.jbee.inject.bootstrap.New;
import se.jbee.inject.bootstrap.Supply;
import se.jbee.inject.config.ConstructionMirror;
import se.jbee.inject.container.Supplier;

/**
 * A {@link Module} that provides a {@link Supplier} for any {@link Extension}.
 * 
 * @since 19.1
 */
public class ExtensionModule extends BinderModule {

	@Override
	protected void declare() {
		ConstructionMirror mirror = bindings().mirrors.construction;
		asDefault() //
				.per(Scope.dependencyType) //
				.bind(raw(Extension.class).asUpperBound()) //
				.toSupplier((dep, context) -> extension(mirror, dep, context));
	}

	@SuppressWarnings("unchecked")
	private static <T> T extension(ConstructionMirror mirror, Dependency<?> dep,
			Injector context) {
		Constructor<T> constructor = (Constructor<T>) mirror.reflect(
				dep.type().rawType);
		return Supply.constructor(New.bind(constructor)).supply(
				(Dependency<? super T>) dep, context);
	}
}
