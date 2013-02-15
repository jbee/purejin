/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.Instance;
import se.jbee.inject.Packages;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Supplier;
import se.jbee.inject.Target;
import se.jbee.inject.Type;

/**
 * The minimal {@link Binder} to bind should illustrate the stages.
 * 
 * @see Binder
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface BasicBinder {

	/**
	 * @return A binder that defines a binding for the given {@link Instance}.
	 */
	<T> TypedBasicBinder<T> bind( Instance<T> instance );

	/**
	 * The {@link Type} of the {@link Resource} defined is already given using
	 * {@link BasicBinder#bind(Instance)}
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	interface TypedBasicBinder<T> {

		/**
		 * @param supplier
		 *            The <i>source</i> that will deliver instances when needed to inject.
		 */
		void to( Supplier<? extends T> supplier );

	}

	/**
	 * The ROOT- {@link RootBasicBinder}.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	public interface RootBasicBinder
			extends ScopedBasicBinder {

		/**
		 * @return A binder that binds within the given {@link Scope}.
		 */
		ScopedBasicBinder per( Scope scope );

	}

	/**
	 * A {@link Scope} had been defined.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	interface ScopedBasicBinder
			extends TargetedBasicBinder {

		/**
		 * @return a binder that has a the given {@link Target} for the bound {@link Resource}.
		 */
		TargetedBasicBinder injectingInto( Instance<?> target );
	}

	/**
	 * Bindings have been restricted and are just effective within a special scope defined before
	 * using {@link ScopedBasicBinder#injectingInto(Instance)}-clauses.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	interface TargetedBasicBinder
			extends BasicBinder {

		/**
		 * @return a binder that whose binds just apply to the given {@link Packages}.
		 */
		BasicBinder in( Packages packages );

	}

}
