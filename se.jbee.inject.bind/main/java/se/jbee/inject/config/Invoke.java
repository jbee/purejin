package se.jbee.inject.config;

import java.lang.reflect.Method;

/**
 * {@link Invoke} is the abstraction of {@link Method#invoke(Object, Object...)}
 * call so that the actual use of reflection can be customised.
 * <p>
 * One way to call methods only visible within their declaring classes package
 * is to bind a local version of {@link Invoke}:
 * <pre>
 * locally().bind(Invoke.class).to(Method::invoke);
 * </pre>
 * <p>
 * Alternatively the implementation of {@link Invoke} could use {@link
 * java.lang.reflect.AccessibleObject#setAccessible(boolean)} which with java
 * module system will require to open the module accordingly.
 */
@FunctionalInterface
public interface Invoke {

	/**
	 * An implementation of this method should basically just call {@link
	 * Method#invoke(Object, Object...)}.
	 * <p>
	 * To work around visibility limitations the implementation of this method
	 * can be defined and bound in the package of the called {@link Method}.
	 * <p>
	 * Alternatively an implementation could for example use {@link
	 * java.lang.reflect.AccessibleObject#setAccessible(boolean)} to make the
	 * {@link Method} accessible before calling it.
	 * <p>
	 * The implementation could also do an entirely different thing as long as
	 * the result is equivalent to calling the provided {@link Method} with the
	 * provided arguments.
	 *
	 * @see Method#invoke(Object, Object...)
	 */
	Object call(Method target, Object instance, Object[] args) throws Exception;
}
