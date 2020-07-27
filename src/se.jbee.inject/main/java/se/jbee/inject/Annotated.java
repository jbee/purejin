package se.jbee.inject;

import static se.jbee.inject.Type.raw;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.UnaryOperator;

import se.jbee.inject.container.Supplier;

/**
 * Can be implemented by {@link Supplier}s to communicate the annotations
 * present on the underlying source used to supply instances.
 * 
 * For example a {@link Method}, {@link Constructor} or {@link Field} but also
 * any form of user defined source.
 * 
 * @author Jan Bernitt
 * 
 * @since 19.1
 */
@FunctionalInterface
public interface Annotated {

	/**
	 * @return the underlying source for annotations
	 */
	AnnotatedElement element();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	Type<UnaryOperator<Annotated>> ENV_AGGREGATOR_KEY = (Type) raw(
			UnaryOperator.class).parametized(Annotated.class);
	UnaryOperator<Annotated> AGGREGATOR = a -> a;

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
