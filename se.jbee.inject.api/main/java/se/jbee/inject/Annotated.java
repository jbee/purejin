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
 * <p>
 * For example a {@link Method}, {@link Constructor} or {@link Field} but also
 * any form of user defined source.
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
	interface Enhancer extends UnaryOperator<Annotated> {
	}

	/**
	 * Keeps the annotation as presented by the source. This is how the {@link
	 * Supplier} presented it from the underlying {@link
	 * java.lang.reflect.Member} or {@link Class}.
	 */
	Enhancer SOURCE = a -> a;

	AnnotatedElement NOT_ANNOTATED = new AnnotatedElement() {

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

	/**
	 * NULL object that represents no {@link Annotation}s present.
	 */
	Annotated EMPTY = () -> NOT_ANNOTATED;

}
