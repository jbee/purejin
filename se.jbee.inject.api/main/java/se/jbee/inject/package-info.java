/**
 * Contains all essential concepts and types of the library. The user facing API
 * is the {@link se.jbee.inject.Injector}.
 *
 * <h2>Code organisation</h2>
 * <p>
 * The root package with it essential types does not depend on any of its
 * sub-packages, the sub-packages depend on the root.
 * </p>
 *
 * <h2>Initialising Objects Created In Context</h2>
 * <p>
 * To initialise objects created in the {@link se.jbee.inject.Injector} bind a
 * {@link se.jbee.inject.Lift} for the {@link se.jbee.lang.Type} of
 * objects it should initialise. This is mostly equivalent to a
 * <code>javax.annotation.PostConstruct</code> annotation just that the effect
 * is not given by an annotated method but by the provided {@link
 * se.jbee.inject.Lift} function. This can equally be applied to the
 * {@link se.jbee.inject.Injector} itself to wrap it. The bootstrapping will
 * return the outermost wrapper.
 * </p>
 */
package se.jbee.inject;
