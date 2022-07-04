/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.lang.Qualifying;
import se.jbee.lang.Type;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashSet;

import static java.util.Arrays.asList;
import static se.jbee.lang.Utils.*;

/**
 * A set of {@link Package}s described one or more root packages (on the same
 * hierarchy level/depth) with or without their sub-packages.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
@SuppressWarnings("squid:S1448")
public final class Packages
		implements Qualifying<Packages>, Serializable, Comparable<Packages> {

	private static final long serialVersionUID = -4308799735424634367L;

	/**
	 * Contains all packages including the (default) package.
	 */
	public static final Packages ALL = new Packages(new String[0], true);

	/**
	 * The (default) package.
	 */
	public static final Packages DEFAULT = new Packages(new String[0], false);

	public static final Packages TEST = new Packages("test.", true);

	public static Packages packageAndSubPackagesOf(Class<?> type) {
		return new Packages(packageNameOf(type), true);
	}

	public static Packages packageAndSubPackagesOf(Package pkg) {
		return new Packages(packageNameOf(pkg), true);
	}

	public static Packages packageAndSubPackagesOf(Class<?> type,
			Class<?>... types) {
		commonPackageDepth(type, types);
		return new Packages(packageNamesOf(type, "", types), true);
	}

	public static Packages packageOf(Class<?> type) {
		return new Packages(packageNameOf(type), false);
	}

	public static Packages packageOf(Class<?> type, Class<?>... types) {
		return new Packages(packageNamesOf(type, "", types), false);
	}

	public static Packages subPackagesOf(Class<?> type) {
		return new Packages(packageNameOf(type) + ".", true);
	}

	public static Packages subPackagesOf(Class<?> type, Class<?>... types) {
		commonPackageDepth(type, types);
		return new Packages(packageNamesOf(type, ".", types), true);
	}

	private static String[] packageNamesOf(Class<?> packageOf, String suffix,
			Class<?>... packagesOf) {
		String[] names = new String[packagesOf.length + 1];
		names[0] = packageNameOf(packageOf) + suffix;
		for (int i = 1; i <= packagesOf.length; i++)
			names[i] = packageNameOf(packagesOf[i - 1]) + suffix;
		return names;
	}

	private static String packageNameOf(Class<?> packageOf) {
		return packageNameOf(packageOf.getPackage());
	}

	private static String packageNameOf(Package pkg) {
		return pkg == null ? "(default)" : pkg.getName();
	}

	private static String packageNameOf(Type<?> packageOf) {
		return packageOf.isUpperBound()
			? "-NONE-"
			: packageNameOf(packageOf.rawType);
	}

	private final String[] roots;
	private final boolean includingSubpackages;
	private final int rootDepth;

	private Packages(String root, boolean includingSubpackages) {
		this(new String[] { root }, includingSubpackages);
	}

	private Packages(String[] roots, boolean includingSubpackages) {
		this.roots = roots;
		this.includingSubpackages = includingSubpackages;
		this.rootDepth = rootDepth(roots);
	}

	public Packages and(Packages further) {
		LinkedHashSet<String> set = new LinkedHashSet<>(asList(roots));
		set.addAll(asList(further.roots));
		return new Packages(set.toArray(new String[0]),
				includingSubpackages || further.includingSubpackages);
	}

	public Packages parents() {
		if (rootDepth == 0)
			return this;
		if (rootDepth == 1)
			return includingSubpackages ? ALL : DEFAULT;
		return new Packages(arrayMap(roots, Packages::parent),
				includingSubpackages);
	}

	/**
	 * <pre>
	 * foo.bar.baz -> foo.bar
	 * foo.bar. -> foo.
	 * </pre>
	 */
	private static String parent(String root) {
		return root.substring(0, root.lastIndexOf('.', root.length() - 2)
			+ (root.endsWith(".") ? 1 : 0));
	}

	private static void commonPackageDepth(Class<?> type, Class<?>[] types) {
		if (arrayContains(types, type, (a, b) -> dotsIn(
				a.getPackage().getName()) != dotsIn(b.getPackage().getName())))
			throw new IllegalArgumentException(
					"All classes of a packages set have to be on same depth level.");
	}

	private static int rootDepth(String[] roots) {
		return roots.length == 0 ? 0 : dotsIn(roots[0]);
	}

	private static int dotsIn(String s) {
		return seqCount(s, '.');
	}

	public boolean contains(Class<?> type) {
		return contains(Type.raw(type));
	}

	public boolean contains(Type<?> type) {
		if (includesAll())
			return true;
		final String packageNameOfType = packageNameOf(type);
		return arrayContains(roots,
				root -> seqRegionEquals(root, packageNameOfType,
						includingSubpackages
							? root.length()
							: packageNameOfType.length()));
	}

	public boolean includesAll() {
		return this == ALL || roots.length == 0 && includingSubpackages;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Packages && equalTo(((Packages) obj));
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(roots);
	}

	@Override
	public boolean moreQualifiedThan(Packages other) {
		if (includingSubpackages != other.includingSubpackages)
			return !includingSubpackages;
		return rootDepth != other.rootDepth && rootDepth > other.rootDepth;
	}

	@Override
	public int compareTo(Packages other) {
		int res = Boolean.compare(includingSubpackages,
				other.includingSubpackages);
		if (res != 0)
			return res;
		res = Integer.compare(rootDepth, other.rootDepth);
		if (res != 0)
			return res;
		return arrayCompare(roots, other.roots);
	}

	@Override
	public String toString() {
		if (roots.length == 0)
			return includingSubpackages ? "*" : "(default)";
		if (roots.length == 1)
			return toString(roots[0]);
		StringBuilder b = new StringBuilder();
		for (String root : roots)
			b.append('+').append(toString(root));
		return b.substring(1);
	}

	private String toString(String root) {
		return root + (includingSubpackages ? "*" : "");
	}

	public boolean equalTo(Packages other) {
		return other.includingSubpackages == includingSubpackages //
			&& other.rootDepth == rootDepth
			&& other.roots.length == roots.length
			&& Arrays.equals(roots, other.roots);
	}

}
