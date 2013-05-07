/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

/**
 * A strategy to create actual artifacts that reflect actual <i>links</i> between an abstract type
 * and its actual implementation(s). This includes the strategy how to construct them (what includes
 * which constructor is picked).
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T>
 *            The type of the artifacts created. This can be any kind of data the is derived from
 *            the input ({@link Bindings}).
 */
public interface Linker<T> {

	/**
	 * Links the {@link Bindings} described by the given {@link Module}s to a list of artifacts that
	 * represent those in some linker specific form.
	 * 
	 * @param inspector
	 *            Default used to pick constructors (and maybe methods used as factories)
	 * @param modules
	 *            A list of {@link Module}s to link together.
	 * @return A list of artifacts resulting from the {@link Module}'s declaration of
	 *         {@link Bindings}.
	 */
	T[] link( Macros macros, Inspector inspector, Module... modules );
}
