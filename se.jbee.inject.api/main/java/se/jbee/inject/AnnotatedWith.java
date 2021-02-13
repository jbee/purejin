package se.jbee.inject;

import se.jbee.inject.Annotated.Enhancer;
import se.jbee.lang.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

/**
 * Allows to resolve bound instances annotated with a certain {@link Annotation}
 * by usual mechanism of resolving a particular {@link Type}.
 *
 * With the {@link AnnotatedWith} resolved the {@link #instances()} method is
 * used to access the instances.
 *
 * This is equivalent to use: {@link Injector#annotatedWith(Class)}
 *
 * @since 8.1
 *
 * @param <A> {@link Annotation} type to resolve, it only exists so a full
 *            generic type can be read using reflection to extract the
 *            annotation type to resolve
 */
@SuppressWarnings("squid:S2326")
@FunctionalInterface
public interface AnnotatedWith<A extends Annotation> {

	/**
	 * Instances referenced in this are supplied by a {@link Supplier} as the
	 * underlying source might allow changes to the result for multiple calls.
	 *
	 * @return The collection of instances that have the annotation {@code T}
	 *         present.
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
	 *         using custom {@link se.jbee.inject.Supplier}s for these
	 *         types (usually done by binding custom
	 *         {@code se.jbee.inject.bind.ValueBinder}s in the {@link Env})
	 *         or in general by using a custom {@link Annotated#SOURCE}
	 *         function which is bound in the {@link Env} for the {@link Enhancer}
	 *         property.
	 */
	List<AnnotatedInstance<?>> instances();

	/**
	 * Record of an instance and the {@link #annotations} present on the element
	 * if was created from. This can be a {@link Constructor}, {@link Method} or
	 * {@link Field} or a custom {@link AnnotatedElement} that implements custom
	 * aggregation of annotation, for example including {@link Class} level
	 * annotations for {@link Constructor}s.
	 *
	 * The {@link #instance} is supplied indirectly as it might be affected by
	 * scoping or underlying sources that return different (updated) result for
	 * each invocation. If this is the case this is reflected by the
	 * {@link Supplier}.
	 */
	final class AnnotatedInstance<T> {

		public final Supplier<T> instance;
		public final Type<? super T> as;
		public final AnnotatedElement annotations;

		public AnnotatedInstance(Supplier<T> instance, Type<? super T> as,
				AnnotatedElement annotations) {
			this.instance = instance;
			this.as = as;
			this.annotations = annotations;
		}
	}
}
