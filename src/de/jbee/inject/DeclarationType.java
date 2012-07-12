package de.jbee.inject;

public enum DeclarationType
		implements PreciserThan<DeclarationType> {

	/**
	 * Has been added by the binder as a fall-back since some bind-calls can have ambiguous
	 * intentions.
	 */
	IMPLICIT,
	/**
	 * Used to provide a default of required parts of a module that can be replaced *once* to
	 * customize behavior.
	 * 
	 * There can be just *one* default for each {@link Resource} and still just one explicit
	 * replacement for it.
	 */
	DEFAULT,
	/**
	 * A auto-bind has been used. That is binding a class or instance to the exact type as
	 * {@link #EXPLICIT} and to all its super-classes and -interfaces as a {@link #AUTO} bound bind.
	 */
	AUTO,
	/**
	 * A bind that is ment to co-exist with other that might have the same {@link Resource}.
	 */
	MULTI,
	/**
	 * The bind has been made explicitly by a module (should be a unique {@link Resource})
	 */
	EXPLICIT;

	@Override
	public boolean morePreciseThan( DeclarationType other ) {
		return ordinal() > other.ordinal();
	}

	public boolean clashesWith( DeclarationType other ) {
		return this == EXPLICIT && other == EXPLICIT;
	}

	public boolean replacedBy( DeclarationType other ) {
		return other.ordinal() > ordinal() || clashesWith( other );
	}

}
