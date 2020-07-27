package se.jbee.inject.bind;

import static java.util.Collections.unmodifiableList;
import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.jbee.inject.AnnotatedWith;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;

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
final class AnnotatedWithModule extends BinderModule {

	@SuppressWarnings("rawtypes")
	private static final Instance<Map> CACHE = Instance.instance(
			named("annotation-cache"), raw(Map.class));

	@Override
	protected void declare() {
		asDefault().bind(CACHE).to(new HashMap<>());
		asDefault() //
				.per(Scope.dependencyType) //
				.bind(anyOf(
						raw(AnnotatedWith.class).parametizedAsUpperBounds())) //
				.toSupplier(AnnotatedWithModule::annotatedWith);
	}

	@SuppressWarnings("unchecked")
	private static <T> AnnotatedWith<?> annotatedWith(Dependency<?> dep,
			Injector context) {
		Map<Class<?>, List<AnnotatedWith.AnnotatedInstance<?>>> cache = context.resolve(
				CACHE);
		return () -> {
			Class<?> annotationType = dep.type().parameter(0).rawType;
			return cache.computeIfAbsent(annotationType, key -> {
				List<AnnotatedWith.AnnotatedInstance<?>> annotated = new ArrayList<>();
				for (Resource<T> r : context.resolve(Resource[].class)) {
					AnnotatedElement element = r.annotations.element();
					if (element.isAnnotationPresent(
							(Class<? extends Annotation>) key)) {
						annotated.add(new AnnotatedWith.AnnotatedInstance<>(
								context.resolve(r.locator.toDependency()),
								r.locator.type().rawType, element));
					}
				}
				return unmodifiableList(annotated);
			});
		};
	}
}