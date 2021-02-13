/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.lang.Qualifying;
import se.jbee.lang.Type;

import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A {@link Name} is used as discriminator in cases where multiple
 * {@link Instance}s are bound for the same {@link Type}.
 */
public final class Name
		implements Qualifying<Name>, Serializable, Comparable<Name> {

	private static final Map<Class<?>, Function<?, String>> ofType = new IdentityHashMap<>();

	public static <T> void registerNaming(Class<T> type, Function<T, String> naming) {
		ofType.put(type, naming);
	}

	static {
		registerNaming(Class.class, Class::getName);
		registerNaming(Package.class, Package::getName);
	}

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

	public static <T> Name named(T name) {
		if (name == null)
			return named("null");
		@SuppressWarnings("unchecked")
		Function<T, String> naming = (Function<T, String>) ofType.get(name.getClass());
		return naming == null
				? named(String.valueOf(name))
				: named(naming.apply(name));
	}

	public static Name named(String name) {
		return name == null || name.trim().isEmpty() ? DEFAULT : new Name(name);
	}

	private Name(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

	public Name in(Name ns) {
		return in(ns.isNamespaced() ? ns.namespace() : ns.value);
	}

	public Name in(String ns) {
		return new Name(ns + NAMESPACE + value);
	}

	public String withoutNamespace() {
		return value.substring(value.indexOf(NAMESPACE) + 1);
	}

	public String namespace() {
		return isNamespaced() ? value.substring(0, value.indexOf(NAMESPACE)) : "";
	}

	public boolean isNamespaced() {
		return value.indexOf(NAMESPACE) >= 0;
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

	public boolean isPattern() {
		return value.contains(WILDCARD+"");
	}

	@Override
	public boolean moreQualifiedThan(Name other) {
		// default is most qualified
		boolean thisIsDefault = isDefault();
		boolean otherIsDefault = other.isDefault();
		if (thisIsDefault || otherIsDefault)
			return !otherIsDefault;
		// any is least qualified
		boolean thisIsAny = isAny();
		boolean otherIsAny = other.isAny();
		if (thisIsAny || otherIsAny)
			return !thisIsAny;
		boolean thisIsPattern = isPattern();
		boolean otherIsPattern = other.isPattern();
		if (thisIsPattern || otherIsPattern)
			return !thisIsPattern;
		return value.length() > other.value.length()
			&& value.startsWith(other.value);
	}

	@Override
	public int compareTo(Name other) {
		return value.compareTo(other.value);
	}

	public boolean isCompatibleWith(Name other) {
		if (isAny()) return true;
		if (equalTo(other)) return true;
		if (other.isAny()) return !isPattern();
		if (!isPattern()) return matches(other.value, value);
		return matches(value, other.value);
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
