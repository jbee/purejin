/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.Env;

/**
 * The basic idea is to split the binding process into 3 steps:
 * <ol>
 * <li>identify the set of installed {@link Bundle}s</li>
 * <li>derive the set of installed {@link Module}s from the installed {@link Bundle}s</li>
 * <li>collect the declared {@link Binding}s within each {@link Module} to a total set of {@link Bindings}</li>
 * </ol>
 * <p>
 * This split allows to keep track of the {@link Module}s that should be installed
 * before actually installing them. This has two major benefits:
 * <ul>
 * <li>It is possible and not conflicting to declare the installation of the same {@link Bundle} and/or {@link Module}
 * multiple times without actually installing them more then once.
 * This allows to see other modules as needed dependencies or 'parent'-modules.
 * This is the basis of composing software modules that do not know about each other but that might share a 3rd software module.</li>
 *
 * <li>Software modules can remove and replace parts of the overall setup by uninstalling {@link Bundle}s and indirectly the {@link Module}s they would have installed.
 * This allows to the the {@link Bundle} tree as a setup where any node or subtree can be replaced without loosing clarity of what the result will look like. Yet through modularisation into {@link Bundle}s and {@link Module}s parts of a removed tree can be reincorporated by installing them as part of another {@link Bundle}.
 * This is the basis of composed software.</li>
 * </ul>
 */
public interface Bootstrapper {

	void installDefaults();

	/**
	 * @param bundle the {@link Bundle} to expand further
	 */
	void install(Class<? extends Bundle> bundle);

	/**
	 * Uninstalling is very different from overriding. It allows to totally
	 * remove a well defined part from the consideration while an override would
	 * complect the overridden and the overriding declarations with each other
	 * and requires to consider both in a complected form.
	 * <p>
	 * To allow predictability uninstalling is a final decision. Further calls
	 * that {@link #install(Class)} for the very same {@link Bundle} will not
	 * re-install it independent of when they occur.
	 * <p>
	 * There is no need to uninstall a {@link Module} since uninstalling the
	 * {@linkplain Bundle} that installed the module will effectively uninstall
	 * the module as well (as long as that module is not installed by another
	 * installed bundle too). Allowing to uninstall separate {@link Module}s
	 * would break the well defined borders of bundles and lead to the need to
	 * consider something close to overrides.
	 * <p>
	 * If an individual {@link Module} should be possible to add or remove wrap
	 * it in a dedicated {@link Bundle} (most {@link Module}s extend a base
	 * class that is a {@link Bundle} as well and therefore can be targeted
	 * individually) or have a look at {@link Dependent} installation.
	 */
	void uninstall(Class<? extends Bundle> bundle);

	/**
	 * @param module the {@link Module} to install (within the parent {@link
	 *               Bundle} that is given implicit - the {@link Bootstrapper}
	 *               keeps track of that)
	 */
	void install(Module module);

	/**
	 * @param elements for the {@link Bundle}s that should be installed.
	 */
	@SuppressWarnings("unchecked")
	<E extends Enum<E> & Dependent<E>> void install(E... elements);

	/**
	 * @param elements for the {@link Bundle}s that are uninstalled.
	 */
	@SuppressWarnings("unchecked")
	<E extends Enum<E> & Dependent<E>> void uninstall(E... elements);

	/**
	 * @param bundle the {@link Bundle} to install
	 * @param dependentOn  The {@link Enum} representing all dependentOn possible for the
	 *               {@link Dependent}
	 */
	<E extends Enum<E>> void install(Class<? extends Dependent<E>> bundle,
			Class<E> dependentOn);

	/**
	 * The "inner" bootstrapper used when
	 *
	 * @param <E> A type used to define a set of possible options. Users can set
	 *            none, one, multiple or all of the options using {@link
	 *            Env#withDependent(Class, Enum[])} (or binds of similar
	 *            effect).
	 */
	@FunctionalInterface
	interface DependentBootstrapper<E> {

		/**
		 * Installs the {@link Bundle} when the given element is {@link
		 * Env#isInstalled(Class, Enum)}.
		 * <p>
		 * The set of flags is always constant during the bootstrapping process
		 * and set using {@link Env#withDependent(Class, Enum[])}
		 *
		 * @param element     when this element is/was set in the {@link Env}
		 * @param bundle a {@link Bundle} to install
		 */
		void installDependentOn(E element, Class<? extends Bundle> bundle);

	}

}
