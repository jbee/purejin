package se.jbee.inject.config;

import java.lang.reflect.Constructor;

/**
 * {@link New} is the abstraction of the {@link Constructor#newInstance(Object...)}
 * call so that the actual use of reflection can be customised.
 * <p>
 * One way to make local classes constructable using reflection is to bind a
 * local version of {@link New}.
 * <pre>
 * locally().bind(New.class).to(Constructor::newInstance);
 * </pre>
 * <p>
 * Alternatively the implementation of {@link New} could use {@link
 * java.lang.reflect.AccessibleObject#setAccessible(boolean)} which with java
 * module system will require to open the module accordingly.
 */
@FunctionalInterface
public interface New {

	<T> T call(Constructor<T> target, Object[] args) throws Exception;

}
