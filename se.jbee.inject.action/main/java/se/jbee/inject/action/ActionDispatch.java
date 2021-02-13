package se.jbee.inject.action;

import java.util.List;

/**
 * An {@link ActionDispatch} implements the algorithm that is used to
 * select and call one or more {@link ActionSite} to produce the {@link Action}
 * output.
 * <p>
 * Different implementations have different strategies to select the {@link
 * ActionSite} to use for each particular call and how to handle different types
 * of errors.
 *
 * @param <A> key input value type (parameter type of the {@link Action})
 * @param <B> output value type (return type of the {@link Action})
 * @see RoundRobinDispatch
 * @see MulticastDispatch
 * @since 8.1
 */
@FunctionalInterface
public interface ActionDispatch<A, B> {

	/**
	 * Executes an {@link ActionSite} by using one or more of the available
	 * {@link ActionSite}s to compute the output value.
	 *
	 * @param input the input value
	 * @param sites a lift of available {@link ActionSite}s, while any of the
	 *              sites might be disconnected at any time the list itself can
	 *              be considered immutable
	 * @return the output value computed by the used {@link ActionSite}
	 * @throws se.jbee.inject.DisconnectException When the strategy could not
	 *                                            compute a result because all
	 *                                            {@link ActionSite} it tried
	 *                                            turned out to be disconnected
	 *                                            in the meantime.
	 */
 	B execute(A input, List<ActionSite<A, B>> sites);
}
