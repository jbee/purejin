package se.jbee.inject;

/**
 * {@link Verifier}s are attached to
 */
@FunctionalInterface
public interface Verifier {

	Verifier AOK = context -> {};

	/**
	 * Called by the {@link Injector} at the end of bootstrapping process for
	 * each {@link Resource} that became part of the container context. This
	 * occurs after the {@link Injector} itself has been initialised by {@link
	 * Lift}s but before eager scoped instances are created.
	 *
	 * @param context the container to use to verify the conditions captured by
	 *                this {@link Verifier}.
	 * @throws InconsistentDeclaration In case the verification concludes that
	 *                                 the context isn't valid. For example a
	 *                                 verifier could check that each of the
	 *                                 parameters of a bound constructor can be
	 *                                 resolved to a {@link Resource}
	 */
	void verifyIn(Injector context) throws InconsistentDeclaration;


	default Verifier and(Verifier other) {
		return context -> {
			verifyIn(context);
			other.verifyIn(context);
		};
	}
}
