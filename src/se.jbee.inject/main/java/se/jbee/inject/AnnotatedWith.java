package se.jbee.inject;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * Allows to resolve bound instances annotated with a certain {@link Annotation}
 * by usual mechanism of resolving a particular {@link Type}.
 * 
 * For example:
 * 
 * <pre>
 * AnnotatedWith<MyAnnotation> annotatedInstances = injector.resolve(
 * 		Type.raw(AnnotatedWith.class).parametized(MyAnnotation.class))
 * </pre>
 * 
 * With the {@link AnnotatedWith} resolved the {@link #annotated()} method is
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
	 * @return a collection (logical set) of instances which all have the
	 *         {@link Annotation} of the type parameter T.
	 */
	Collection<?> annotated();

	//TODO virtual annotations - that is pretending a class was annotated by making a plugin entry
}
