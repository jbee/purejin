package se.jbee.inject.bind;

import se.jbee.inject.*;

/**
 * {@link BindingConsolidation} is the process of verifying the consistency of a
 * given set of {@link Binding}s, remove those {@link Binding}s that clash but
 * were not explicitly made and sort the {@link Binding}s in an order that
 * groups them by {@link se.jbee.inject.Supplier#supply(Dependency, Injector)}
 * return type and from the most qualified to the lest qualified within each
 * type group.
 * <p>
 * This process should occur once  all {@link Binding}s have been expanded from
 * the declaration {@link Bundle}s and {@link Module}s.
 *
 * @see DeclarationType for more details
 * @since 8.1
 */
public interface BindingConsolidation {

	/**
	 * Returns the consolidated, that means sorted and disambiguated list of
	 * {@link Binding}s.
	 * <p>
	 * If the input does not allow to determine such a list it fails throwing an
	 * {@link InconsistentBinding} exception.
	 * <p>
	 * If {@link Binding}s of type {@link BindingType#REQUIRED} were made that
	 * are not satisfied by any of the bindings in the given set a {@link
	 * se.jbee.inject.UnresolvableDependency} exception is thrown.
	 *
	 * @param env      The configuration to use to read properties that
	 *                 customise the consolidation process.
	 * @param declared the set of {@link Binding}s that was the output of the
	 *                 {@link Bundle} and {@link Module} expansion (or whatever
	 *                 source was used). These represent a bag, that means no
	 *                 particular order is required and duplicates are
	 *                 permitted.
	 * @return a list containing the elements of the provided set after that has
	 * been disambiguated and sorted. This may or may not be the provided set
	 * itself if appropriate. This may or may not sort the provided set in place
	 * or return a new array.
	 * @throws InconsistentBinding    when bindings in the provided set were
	 *                                ambiguous but explicitly defined
	 * @throws UnresolvableDependency when a required binding wasn't satisfied
	 *                                by any of the other bindings in the set
	 *                                (after it had been disambiguated). This
	 *                                can mean there were matching bindings in
	 *                                the set but they were ambiguous and not
	 *                                explicit so all of them got removed.
	 */
	Binding<?>[] consolidate(Env env, Binding<?>[] declared);
}
