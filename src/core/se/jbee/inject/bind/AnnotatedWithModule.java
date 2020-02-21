package se.jbee.inject.bind;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.raw;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import se.jbee.inject.AnnotatedWith;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.config.Plugins;
import se.jbee.inject.container.Supplier;

/**
 * Provides a {@link Supplier} that can resolve all {@link AnnotatedWith}
 * dependencies.
 * 
 * This allows to inject a {@link Collection} of instances which are annotated
 * with the {@link Annotation} referenced by the type parameter of
 * {@link AnnotatedWith}.
 * 
 * The {@link Injector} provides a convenience method to access the resolved
 * instances using {@link Injector#annotatedWith(Class)}. This will only work if
 * this module is installed.
 * 
 * @since 19.1
 */
public class AnnotatedWithModule extends BinderModule {

	@Override
	protected void declare() {
		asDefault() //
				.per(Scope.dependencyType) //
				.bind(anyOf(
						raw(AnnotatedWith.class).parametizedAsUpperBounds())) //
				.toSupplier(AnnotatedWithModule::annotatedWith);
	}

	private static AnnotatedWith<?> annotatedWith(Dependency<?> dep,
			Injector context) {
		return () -> {
			Class<?> annotation = dep.type().parameter(0).rawType;
			Class<?>[] annotated = context.resolve(Plugins.class).forPoint(
					annotation);
			List<Object> res = new ArrayList<>();
			for (Class<?> a : annotated)
				res.add(context.resolve(a));
			return res;
		};
	}
}
