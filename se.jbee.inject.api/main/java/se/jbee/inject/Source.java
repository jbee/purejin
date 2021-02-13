/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.lang.Qualifying;

import java.io.Serializable;

/**
 * Where does a bind come from and what type of declaration has it been.
 */
public final class Source
		implements Qualifying<Source>, Serializable, Comparable<Source> {

	public static Source source(Class<?> module) {
		return new Source(module, DeclarationType.EXPLICIT, 0, 0);
	}

	public final Class<?> ident;
	public final DeclarationType declarationType;
	public final int declarationNo;

	/**
	 * Number of declarations from this source in total.
	 */
	private int totalDeclarations;

	private Source(Class<?> ident, DeclarationType declarationType,
			int declarationNo, int totalDeclarations) {
		this.ident = ident;
		this.declarationType = declarationType;
		this.declarationNo = declarationNo;
		this.totalDeclarations = totalDeclarations;
	}

	@Override
	public String toString() {
		return ident.getSimpleName() + "#" + declarationNo
			+ ":" + declarationType.name().charAt(0);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Source && equalTo((Source) obj);
	}

	public boolean equalTo(Source other) {
		return ident == other.ident && declarationType == other.declarationType
			&& declarationNo == other.declarationNo;
	}

	@Override
	public int hashCode() {
		// declarationType should not play a role as there should only be
		// one Source with same declarationNo in an ident
		return ident.hashCode() ^ declarationNo;
	}

	@Override
	public boolean moreQualifiedThan(Source other) {
		return declarationType.moreQualifiedThan(other.declarationType);
	}

	@Override
	public int compareTo(Source other) {
		int res = declarationType.compareTo(other.declarationType);
		if (res != 0)
			return res;
		res = ident.getName().compareTo(other.ident.getName());
		if (res != 0)
			return res;
		return Integer.compare(declarationNo, other.declarationNo);
	}

	public Source typed(DeclarationType type) {
		return declarationType == type
			? this
			: new Source(ident, type, declarationNo, totalDeclarations);
	}

	public Source next() {
		totalDeclarations++;
		return declarationNo > 0
			? this
			: new Source(ident, declarationType, totalDeclarations, 0);
	}

}
