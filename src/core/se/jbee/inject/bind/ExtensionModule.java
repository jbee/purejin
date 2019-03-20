package se.jbee.inject.bind;

import static se.jbee.inject.Type.raw;

import java.lang.reflect.Constructor;

import se.jbee.inject.Dependency;
import se.jbee.inject.Extension;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.bootstrap.BoundConstructor;
import se.jbee.inject.bootstrap.Supply;
import se.jbee.inject.config.ConstructionMirror;
import se.jbee.inject.container.Supplier;

public class ExtensionModule extends BinderModule {

	@Override
	protected void declare() {
		asDefault().per(Scope.dependencyType).bind(
				raw(Extension.class).asUpperBound()).toSupplier(
						new ExtensionSupplier<>(
								bindings().mirrors.construction));
	}

	static final class ExtensionSupplier<T extends Extension>
			implements Supplier<T> {

		final ConstructionMirror construction;

		ExtensionSupplier(ConstructionMirror construction) {
			this.construction = construction;
		}

		@Override
		public T supply(Dependency<? super T> dep, Injector injector) {
			@SuppressWarnings("unchecked")
			Constructor<T> c = (Constructor<T>) construction.reflect(
					dep.type().rawType);
			return Supply.constructor(BoundConstructor.bind(c)).supply(dep,
					injector);
		}

	}
}
