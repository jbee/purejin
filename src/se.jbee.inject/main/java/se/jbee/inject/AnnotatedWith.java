package se.jbee.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Allows to resolve bound instances annotated with a certain {@link Annotation}
 * by usual mechanism of resolving a particular {@link Type}.
 * 
 * For example:
 * 
 * <pre>
 * List<AnnotatedInstance> annotatedInstances = injector.resolve(
 * 		Type.raw(AnnotatedWith.class).parametized(
 * 				MyAnnotation.class)).instances();
 * // is same as the shorthand:
 * List<AnnotatedInstance> annotatedInstances = injector.annotatedWith(
 * 		MyAnnotation.class);
 * </pre>
 * 
 * With the {@link AnnotatedWith} resolved the {@link #instances()} method is
 * used to access the instances.
 * 
 * This is equivalent to use: {@link Injector#annotatedWith(Class)}
 *
 * @since 19.1
 *
 * @param <T> {@link Annotation} type to resolve
 */
@FunctionalInterface
public interface AnnotatedWith<T extends Annotation> {

	/**
	 * @return The list of instances that have the annotation {@code T} present.
	 * 
	 *         By default this means the annotation must be present at the
	 *         relevant location that supplies the instance. For example if the
	 *         instance is created from a {@link Constructor} the annotation
	 *         must be present on that {@link Constructor}, if it is produced by
	 *         a factory {@link Method} the annotation must be present on that
	 *         method, or if it is shared from a {@link Field} the annotation
	 *         must be present on the {@link Field}.
	 * 
	 *         This default behaviour can be customised on individual level by
	 *         using custom {@link se.jbee.inject.container.Supplier}s for these
	 *         types (usually done by binding custom
	 *         {@link se.jbee.inject.declare.ValueBinder}s in the {@link Env})
	 *         or in general by using a custom {@link Annotated#AGGREGATOR}
	 *         function which is bound in the {@link Env} using the
	 *         {@link Annotated#ENV_AGGREGATOR_KEY}.
	 */
	List<AnnotatedInstance<?>> instances();

	/**
	 * Record of an instance and the {@link #annotations} present on the element
	 * if was created from. This can be a {@link Constructor}, {@link Method} or
	 * {@link Field} or a custom {@link AnnotatedElement} that implements custom
	 * aggregation of annotation, for example including {@link Class} level
	 * annotations for {@link Constructor}s.
	 */
	final class AnnotatedInstance<T> {

		public final T instance;
		public final Class<? super T> role;
		public final AnnotatedElement annotations;

		public AnnotatedInstance(T instance, Class<? super T> role,
				AnnotatedElement annotations) {
			this.instance = instance;
			this.role = role;
			this.annotations = annotations;
		}
	}
}
