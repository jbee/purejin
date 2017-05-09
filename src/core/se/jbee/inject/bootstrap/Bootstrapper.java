/*
 *  Copyright (c) 2012-2017, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.config.Options;

/**
 * The basic idea is to split the binding process into 2 steps: installing modules and do bindings
 * in the installed modules.
 * 
 * Thereby it is possible to keep track of the modules that should be installed before actually
 * install them. This has two major benefits:
 * 
 * 1. it is possible and intentional to declare installation of the same module as often as wanted
 * or needed without actually installing them more then once. This allows to see other modules as
 * needed dependencies or 'parent'-modules.
 * 
 * 2. the installation can be the first step of the verification (in a unit-test). The binding can
 * be omitted so that overall test of a configuration can be very fast.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 */
public interface Bootstrapper {

	/**
	 * @param bundle
	 *            the {@link Bundle} to expand further
	 */
	void install( Class<? extends Bundle> bundle );

	/**
	 * Uninstalling is very different from overriding. It allows to totally remove a well defined
	 * part from the consideration while a override complects the overridden and the overriding
	 * {@linkplain Bundle} with each other and requires to considerer both in a complected form.
	 * 
	 * To allow predictability uninstalling is a final decision. Further calls that install the very
	 * same {@link Bundle} will not re-install it!
	 * 
	 * There is no need to uninstall a {@link Module} since uninstalling the {@linkplain Bundle}
	 * that installed the module will effectively uninstall the module as well (as long as that
	 * module is not installed by another installed bundle too). Allowing to uninstall separate
	 * {@link Module}s would break the well defined borders of bundles and lead to the need to
	 * consider something close to overrides.
	 */
	void uninstall( Class<? extends Bundle> bundle );

	/**
	 * @param module
	 *            the {@link Module} to install (within the parent {@link Bundle} that is given
	 *            implicit - the {@link Bootstrapper} keeps track of that)
	 */
	void install( Module module );

	/**
	 * @param <T>
	 *            Type of the preset value passed to the module
	 * @param module
	 *            the {@link PresetModule} to install (within the parent {@link Bundle} that is
	 *            given implicitly - the {@link Bootstrapper} keeps track of that)
	 */
	<T> void install( PresetModule<T> module );

	/**
	 * @param options
	 *            The choices made to install.
	 */
	<O extends Enum<O> & OptionBundle<O>> void install( @SuppressWarnings("unchecked") O... options );

	/**
	 * @param options
	 *            The choices made to uninstall again.
	 */
	<O extends Enum<O> & OptionBundle<O>> void uninstall( @SuppressWarnings("unchecked") O... options );

	/**
	 * @see Options
	 * @param bundle
	 *            the {@link Bundle} to install
	 * @param optionProperty
	 *            The property the installation is connected to.
	 */
	<O extends Enum<O>> void install( Class<? extends OptionBundle<O>> bundle, Class<O> optionProperty );

	/**
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 * 
	 * @param <O>
	 *            The type of choices possible
	 */
	@FunctionalInterface
	interface OptionBootstrapper<O> {

		/**
		 * Installs the bundle when the given option is used.
		 * 
		 * If the module passed hasn't been {@link Bootstrapper#install(Enum...)}ed the call will be
		 * ignored.
		 */
		void install( Class<? extends Bundle> bundle, O onOption );
	}

}
