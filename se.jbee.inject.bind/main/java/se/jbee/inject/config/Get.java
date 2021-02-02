package se.jbee.inject.config;

import java.lang.reflect.Field;

/**
 * {@link Get} is the abstraction of {@link Field#get(Object)}
 * call so that the actual use of reflection can be customised.
 * <p>
 * One way to access a field only visible within their declaring classes package
 * is to bind a local version of {@link Get}:
 * <pre>
 * locally().bind(Get.class).to(Field::get);
 * </pre>
 * <p>
 * Alternatively the implementation of {@link Get} could use {@link
 * java.lang.reflect.AccessibleObject#setAccessible(boolean)} which with java
 * module system will require to open the module accordingly.
 */
@FunctionalInterface
public interface Get {

	/**
	 * An implementation of this method should basically just call {@link
	 * Field#get(Object)}.
	 * <p>
	 * To work around visibility limitations the implementation of this method
	 * can be defined and bound in the package of the called {@link Field}.
	 * <p>
	 * Alternatively an implementation could for example use {@link
	 * java.lang.reflect.AccessibleObject#setAccessible(boolean)} to make the
	 * {@link Field} accessible before accessing it.
	 * <p>
	 * The implementation could also do an entirely different thing as long as
	 * the result is equivalent to accessing the provided {@link Field} of the
	 * provided instance.
	 *
	 * @see Field#get(Object)
	 */
	Object call(Field target, Object instance) throws Exception;
}
