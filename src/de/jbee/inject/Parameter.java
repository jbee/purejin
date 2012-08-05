package de.jbee.inject;

import java.lang.reflect.Constructor;

/**
 * {@linkplain Parameter}s are *not* about to find/identify the {@link Constructor} to use! This
 * would get far to complex to understand (yet simple to implement) using different constructors
 * depending on the {@linkplain Parameter}s.
 * 
 * {@linkplain Parameter}s identify the {@link Instance} to use for the constructor specified or
 * given by {@link InjectionStrategy}!
 * 
 * <h3>How {@linkplain Parameter}s are understood:</h3>
 * <dl>
 * <dt>A {@link Class}</dt>
 * <dd>Use the default instance of the given {@linkplain Class}</dd>
 * <dt>A {@link Type}</dt>
 * <dd>Use the default instance of the given {@linkplain Type} (needed for generic classes)</dd>
 * <dt>A {@link Instance}</dt>
 * <dd>Use the instance identified by the given {@linkplain Instance}</dd>
 * <dt>A {@link Dependency}</dt>
 * <dd>Use the instance resolved by the given {@linkplain Dependency} (finest level of control)</dd>
 * <dt>A instance {@link Object}</dt>
 * <dd>Use the given {@linkplain Object} (for the first parameter it is assignable to)</dd>
 * </dl>
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Parameter<T> {

	boolean isAssignableTo( Type<?> type );
}
