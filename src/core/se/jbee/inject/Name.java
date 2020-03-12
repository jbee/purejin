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
	 * Character used as wild-card when matching names.
	 */
	private static final char WILDCARD = '*';

	/**
	 * Character used as divider for name-spacing {@link Class} objects passed
	 * to {@link #named(Object)}.
	 */
	public static final char NAMESPACE = ':';

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
	public static final Name ANY = new Name("" + WILDCARD);

	public static final Name AS = new Name("as");

	final String value;

	public static Name named(Object name) {
		if (name instanceof Class) {
			Class<?> cls = (Class<?>) name;
			return named(cls.getCanonicalName() + NAMESPACE);
		}
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

	public String withoutNamespace() {
		return value.substring(value.indexOf(NAMESPACE) + 1);
	}

	public Name asPrefix() {
		return isAny() ? this : named(value + WILDCARD);
	}

	public Name concat(String suffix) {
		return named(value + suffix);
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
		if (thisIsDefault || otherIsDefault)
			return !otherIsDefault;
		final boolean thisIsAny = isAny();
		final boolean otherIsAny = other.isAny();
		if (thisIsAny || otherIsAny)
			return !thisIsAny;
		return value.length() > other.value.length()
			&& value.startsWith(other.value);
	}

	public boolean isCompatibleWith(Name other) {
		return isAny() || other.isAny() || other.value.equals(value)
			|| matches(value, other.value) || matches(other.value, value);
	}

	private static boolean matches(String pattern, String str) {
		int i = 0;
		int j = 0;
		int len = pattern.length();
		int strLen = str.length();
		while (i < len && j < strLen) {
			char p = pattern.charAt(i++);
			if (p == WILDCARD) {
				if (i == len) // end of pattern is wild-card
					return true;
				while (str.charAt(j) != pattern.charAt(i)) // i already forwarded to next
					j++;
			} else if (p != str.charAt(j)) {
				return false;
			} else {
				j++;
			}
		}
		return j == strLen && (i == len
			|| i + 1 == len && pattern.charAt(len - 1) == WILDCARD);
	}

}
