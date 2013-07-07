/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * A {@link Name} is used as discriminator in cases where multiple {@link Instance}s are bound for
 * the same {@link Type}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Name
		implements PreciserThan<Name> {

	private static final char INTERNAL = '-';

	/**
	 * Character used as wildcard when matching names.
	 */
	public static final String WILDCARD = "*";

	/**
	 * Used when no name is specified. It is the most precise name of all.
	 */
	public static final Name DEFAULT = new Name( "" );
	/**
	 * It is the least precise name of all.
	 */
	public static final Name ANY = new Name( WILDCARD );

	private final String value;

	/**
	 * @see #namedInternal(String)
	 */
	public static Name named( String name ) {
		if ( name == null || name.trim().isEmpty() ) {
			return DEFAULT;
		}
		if ( isInternal( name ) ) {
			throw new IllegalArgumentException(
					"Names starting with a minus are considered to be internal names. If you meant to create such a name use method 'namedInternal' instead." );
		}
		return new Name( name.toLowerCase() );
	}

	/**
	 * Internal names use a special prefix to avoid name clashes with 'usual' user defined names.
	 * They should be used for names that the user does not directly know about.
	 * 
	 * @param name
	 *            A value having the {@link #INTERNAL} prefix '-' or not.
	 * @return The name instance having the {@link #INTERNAL} prefix in any case.
	 */
	public static Name namedInternal( String name ) {
		return isInternal( name )
			? new Name( name )
			: new Name( INTERNAL + name );
	}

	public static Name namedInternal( Enum<?> name ) {
		return name == null
			? namedInternal( "-default-" )
			: namedInternal( name.name().toLowerCase().replace( '_', INTERNAL ) );
	}

	private Name( String value ) {
		super();
		this.value = value.intern();
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	public boolean equalTo( Name other ) {
		return value == other.value;
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Name && equalTo( (Name) obj );
	}

	public boolean isAny() {
		return value.equals( ANY.value );
	}

	public boolean isDefault() {
		return value.isEmpty();
	}

	public boolean isInternal() {
		return isInternal( value );
	}

	private static boolean isInternal( String name ) {
		return name.length() > 0 && name.charAt( 0 ) == INTERNAL;
	}

	@Override
	public boolean morePreciseThan( Name other ) {
		final boolean thisIsDefault = isDefault();
		final boolean otherIsDefault = other.isDefault();
		if ( thisIsDefault || otherIsDefault ) {
			return !otherIsDefault;
		}
		final boolean thisIsAny = isAny();
		final boolean otherIsAny = other.isAny();
		if ( thisIsAny || otherIsAny ) {
			return !thisIsAny;
		}
		return value.length() > other.value.length() && value.startsWith( other.value );
	}

	public boolean isApplicableFor( Name other ) {
		return isAny() || other.isAny() || other.value == value
				|| ( value.matches( other.value.replace( WILDCARD, ".*" ) ) );
	}

	public static Name namedBy( Class<? extends Annotation> annotation, AnnotatedElement obj ) {
		return annotation == null || !obj.isAnnotationPresent( annotation )
			? Name.DEFAULT
			: namedBy( annotation, obj.getAnnotation( annotation ) );
	}

	public static Name namedBy( Class<? extends Annotation> annotation, Annotation... instances ) {
		for ( Annotation i : instances ) {
			if ( i.annotationType() == annotation ) {
				return namedBy( annotation, i );
			}
		}
		return Name.DEFAULT;
	}

	private static Name namedBy( Class<? extends Annotation> annotation, Annotation instance ) {
		for ( Method m : annotation.getDeclaredMethods() ) {
			if ( String.class == m.getReturnType() ) {
				String name = null;
				try {
					name = (String) m.invoke( instance );
				} catch ( Exception e ) {
					// try next...
				}
				if ( name != null && !name.isEmpty() && !name.equals( m.getDefaultValue() ) ) {
					return Name.named( name );
				}
			}
		}
		return Name.DEFAULT;
	}
}
