package se.jbee.inject.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * A {@link Metaclass} is a meta representation of a {@link Class} that allows to analyze it in
 * terms of ideas in the context of 'kinds' or 'meta-classes' in type theory.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Metaclass {

	public final static Metaclass metaclass( Class<?> cls ) {
		return new Metaclass( cls );
	}

	private final Class<?> cls;

	private Metaclass( Class<?> cls ) {
		super();
		this.cls = cls;
	}

	/**
	 * A {@link Class} is monomodal if it there is just a single possible initial state. All newly
	 * created instances can just have this similar initial state but due to internal state they
	 * could develop (behave) different later on.
	 * 
	 * The opposite of monomodal is multimodal.
	 */
	public boolean monomodal() {
		if ( cls.isInterface() ) {
			return false;
		}
		if ( cls == Object.class ) {
			return true;
		}
		for ( Field f : cls.getDeclaredFields() ) {
			if ( !Modifier.isStatic( f.getModifiers() ) ) {
				return false;
			}
		}
		for ( Constructor<?> c : cls.getDeclaredConstructors() ) {
			if ( c.getParameterTypes().length > 0 ) {
				// maybe args are passed to super-type so we check this
				return metaclass( cls.getSuperclass() ).monomodal();
			}
		}
		return true;
	}

	public final boolean undeterminable() {
		return cls.isInterface() || cls.isEnum() || cls.isPrimitive() || cls.isArray()
				|| Modifier.isAbstract( cls.getModifiers() ) || cls == String.class
				|| Number.class.isAssignableFrom( cls ) || cls == Boolean.class
				|| cls == Void.class || cls == void.class;
	}

	//monovalent
}
