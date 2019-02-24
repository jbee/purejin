/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

/**
 * A {@link Metaclass} is a meta representation of a {@link Class} that allows
 * to analyze it in terms of ideas in the context of 'kinds' or 'meta-classes'
 * in type theory.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Metaclass {

	public static Metaclass metaclass(Class<?> cls) {
		return new Metaclass(cls);
	}

	private final Class<?> cls;

	private Metaclass(Class<?> cls) {
		this.cls = cls;
	}

	/**
	 * @return A {@link Class} is monomodal if it there is just a single
	 *         possible initial state. All newly created instances can just have
	 *         this similar initial state but due to internal state they could
	 *         (not necessarily must) develop (behave) different later on.
	 *
	 *         The opposite of monomodal is multimodal.
	 */
	public boolean monomodal() {
		if (cls.isInterface())
			return false;
		if (cls == Object.class)
			return true;
		for (Field f : cls.getDeclaredFields()) {
			if (!Modifier.isStatic(f.getModifiers())) {
				return false;
			}
		}
		for (Constructor<?> c : cls.getDeclaredConstructors()) {
			if (c.getParameterTypes().length > 0) {
				// maybe arguments are passed to super-type so we check it too
				return metaclass(cls.getSuperclass()).monomodal();
			}
		}
		return true;
	}

	/**
	 * @return A {@link Class} is indeterminable when there is no determinable
	 *         way to create instances. This is true for all value types, enums,
	 *         collection types (including arrays) or any type than cannot be
	 *         instantiated by its nature (abstract types).
	 *
	 *         Note that this method just covers those types that are *known* to
	 *         be indeterminable. There will be a lot of user defined types that
	 *         are indeterminable as well but which will not return true.
	 */
	public final boolean undeterminable() {
		return cls.isInterface() || cls.isEnum() || cls.isPrimitive()
			|| cls.isArray() || Modifier.isAbstract(cls.getModifiers())
			|| cls == String.class || Number.class.isAssignableFrom(cls)
			|| cls == Boolean.class || cls == Void.class || cls == void.class
			|| cls == Class.class || Collection.class.isAssignableFrom(cls);
	}

	/**
	 * @return the given object made accessible.
	 */
	public static <T extends AccessibleObject> T accessible(T obj) {
		obj.setAccessible(true);
		return obj;
	}

}
