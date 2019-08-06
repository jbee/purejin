/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * {@linkplain Parameter}s identify what to inject as argument to a
 * {@link Constructor} or {@link Method} parameter.
 * 
 * {@linkplain Parameter}s are *not* about finding or identifying the
 * {@link Constructor} to use!
 * 
 * <h3>How {@linkplain Parameter}s are understood:</h3>
 * <dl>
 * <dt>A {@link Class} (via Type)</dt>
 * <dd>Use the default instance of the given {@linkplain Class}</dd>
 * <dt>A {@link Type}</dt>
 * <dd>Use the default instance of the given {@linkplain Type} (needed for
 * generic classes)</dd>
 * <dt>An {@link Instance}</dt>
 * <dd>Use the instance identified by the given {@linkplain Instance}</dd>
 * <dt>A {@link Dependency}</dt>
 * <dd>Use the instance resolved by the given {@linkplain Dependency} (finest
 * level of control)</dd>
 * <dt>An instance {@link Object}</dt>
 * <dd>Use the given {@linkplain Object} (for the first parameter it is
 * assignable to)</dd>
 * </dl>
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface Parameter<T> extends Typed<T> {

	Parameter<?>[] noParameters = new Parameter<?>[0];

	Hint<T> asHint();

	default <S> Hint<S> asType(Type<S> supertype) {
		return asHint().asType(supertype);
	}

	default <S> Hint<S> asType(Class<S> supertype) {
		return asHint().asType(supertype);
	}
}
