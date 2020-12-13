/**
 * <p>
 * This package contains tests that use the fluent {@link
 * se.jbee.inject.binder.Binder} API to setup a test scenario.
 * <p>
 * This means almost all of them bootstrap an {@link se.jbee.inject.Injector}
 * context for that test scenario and verify the behaviour of the resulting
 * {@link se.jbee.inject.Injector}. Supposedly this is best described as
 * component tests. They test all involved parts from the user facing fluent API
 * to declare the test scenario to the inner workings of the {@link
 * se.jbee.inject.Injector} to verify the correct behaviour.
 * </p>
 * <p>
 * The tests are grouped in 4 groups:
 * </p>
 *
 * <h2>Basics</h2>
 * <p>
 * Use the name pattern {@code TestBasic*}. They show-case the basics of the
 * fluent API and their effects in small self-contained scenarios focussing on a
 * single concept or type of binding. They are also meant as a beginners guide
 * to learn the fundamentals of the library.
 * </p>
 *
 * <h2>Examples</h2>
 * <p>
 * Use the name pattern {@code TestExample*}. They show-case how to solve a
 * particular problem in mostly small self-contained examples. They are also
 * meant as a reference or template that can be analysed and adopted to the
 * users problem.
 * </p>
 *
 * <h2>Features</h2>
 * <p>
 * Use the name pattern {@code TestFeature*}. They are meant to verify the
 * correct behaviour of a single feature. As such they can be used as a
 * reference for that particular feature.
 * </p>
 */
package test.integration.bind;
