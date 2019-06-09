/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.io.Serializable;

/**
 * A {@link Name} is used as discriminator in cases where multiple
 * {@link Instance}s are bound for the same {@link Type}.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Name implements Qualifying<Name>, Serializable {

	/**
	 * Character used as wildcard when matching names.
	 */
	private static final String WILDCARD = "*";

	/**
	 * Used when no name is specified. Maybe at first counter-intuitively this
	 * is the most qualified name of all because it is the first to try. If the
	 * dependency asks for a specific name the default name will not match an
	 * thus continue trying less qualifying names.
	 * 
	 * @see #ANY
	 */
	public static final Name DEFAULT = new Name("");
	/**
	 * It is the least qualified name of all so it is the last name that will be
	 * tried and as it matches any name asked for it the match is found.
	 */
	public static final Name ANY = new Name(WILDCARD);

	final String value;

	public static Name named(Object name) {
		return named(String.valueOf(name));
	}

	public static Name named(String name) {
		return name == null || name.trim().isEmpty()
			? DEFAULT
			: new Name(name.toLowerCase());
	}

	private Name(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	public boolean equalTo(Name other) {
		return value.equals(other.value);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Name && equalTo((Name) obj);
	}

	public boolean isAny() {
		return value.equals(ANY.value);
	}

	public boolean isDefault() {
		return value.isEmpty();
	}

	@Override
	public boolean moreQualiedThan(Name other) {
		final boolean thisIsDefault = isDefault();
		final boolean otherIsDefault = other.isDefault();
		if (thisIsDefault || otherIsDefault) {
			return !otherIsDefault;
		}
		final boolean thisIsAny = isAny();
		final boolean otherIsAny = other.isAny();
		if (thisIsAny || otherIsAny) {
			return !thisIsAny;
		}
		return value.length() > other.value.length()
			&& value.startsWith(other.value);
	}

	public boolean isCompatibleWith(Name other) {
		return isAny() || other.isAny() || other.value.equals(value)
			|| value.matches(other.value.replace(WILDCARD, ".*"))
			|| other.value.matches(value.replace(WILDCARD, ".*"));
	}

}
