package se.jbee.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.UnaryOperator;

/**
 * Can be implemented by {@link Supplier}s to communicate the annotations
 * present on the underlying source used to supply instances.
 *
 * For example a {@link Method}, {@link Constructor} or {@link Field} but also
 * any form of user defined source.
 *
 * @author Jan Bernitt
 *
 * @since 8.1
 */
@FunctionalInterface
public interface Annotated {

	/**
	 * @return the underlying source for annotations
	 */
	AnnotatedElement element();

	@FunctionalInterface
	interface Merge extends UnaryOperator<Annotated> {
	}

	Merge NO_MERGE = a -> a;

	Annotated WITH_NO_ANNOTATIONS = () -> new AnnotatedElement() {

		@Override
		public <T extends Annotation> T getAnnotation(Class<T> type) {
			return null;
		}

		@Override
		public Annotation[] getAnnotations() {
			return new Annotation[0];
		}

		@Override
		public Annotation[] getDeclaredAnnotations() {
			return new Annotation[0];
		}

	};
}
