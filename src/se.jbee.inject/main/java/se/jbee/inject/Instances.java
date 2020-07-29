/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static java.util.Arrays.asList;
import static se.jbee.inject.Utils.arrayPrepand;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A hierarchy of {@link Instance}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Instances implements Qualifying<Instances>,
		Iterable<Instance<?>>, Serializable, Comparable<Instances> {

	public static final Instances ANY = new Instances();

	private final Instance<?>[] hierarchy;

	private Instances(Instance<?>... hierarchy) {
		this.hierarchy = hierarchy;
	}

	public boolean isAny() {
		return hierarchy.length == 0;
	}

	public int depth() {
		return hierarchy.length;
	}

	public Instance<?> at(int depth) {
		return hierarchy[depth];
	}

	public Instances push(Instance<?> top) {
		if (isAny())
			return new Instances(top);
		return new Instances(arrayPrepand(top, hierarchy));
	}

	public boolean equalTo(Instances other) {
		return this == other || Arrays.equals(other.hierarchy, hierarchy);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Instances && equalTo((Instances) obj);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(hierarchy);
	}

	@Override
	public String toString() {
		if (isAny())
			return "*";
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < hierarchy.length; i++) {
			if (i > 0)
				b.append(" => ");
			b.append(hierarchy[i]);
		}
		return b.toString();
	}

	@Override
	public Iterator<Instance<?>> iterator() {
		return asList(hierarchy).iterator();
	}

	/**
	 * @return true when this has higher depth or equal depth and the first more
	 *         precise hierarchy element.
	 */
	@Override
	public boolean moreQualiedThan(Instances other) {
		if (this == other)
			return false;
		if (hierarchy.length != other.hierarchy.length) {
			return hierarchy.length > other.hierarchy.length;
		}
		for (int i = 0; i < hierarchy.length; i++) {
			if (hierarchy[i].moreQualiedThan(other.hierarchy[i]))
				return true;
			if (other.hierarchy[i].moreQualiedThan(hierarchy[i]))
				return false;
		}
		return false;
	}

	@Override
	public int compareTo(Instances other) {
		return Arrays.compare(hierarchy, other.hierarchy, Instance::compareTo);
	}
}
