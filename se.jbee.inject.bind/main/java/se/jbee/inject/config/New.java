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

	/**
	 * An implementation of this method should basically just call {@link
	 * Constructor#newInstance(Object...)}.
	 * <p>
	 * To work around visibility limitations the implementation of this method
	 * can be defined and bound in the package of the called {@link
	 * Constructor}.
	 * <p>
	 * Alternatively an implementation could for example use {@link
	 * java.lang.reflect.AccessibleObject#setAccessible(boolean)} to make the
	 * {@link Constructor} accessible before calling it.
	 * <p>
	 * The implementation could also do an entirely different thing as long as
	 * the result is equivalent to calling the provided {@link Constructor} with
	 * the provided arguments.
	 *
	 * @see Constructor#newInstance(Object...)
	 */
	<T> T call(Constructor<T> target, Object[] args) throws Exception;
}
