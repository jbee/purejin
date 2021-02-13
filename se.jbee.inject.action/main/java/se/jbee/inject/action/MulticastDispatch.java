package se.jbee.inject.action;

import se.jbee.inject.DisconnectException;
import se.jbee.inject.Injector;

import java.util.List;

/**
 * The {@link MulticastDispatch} calls all {@link ActionSite}s and returns the
 * last successful result.
 */
public final class MulticastDispatch<A, B> implements
		ActionDispatch<A, B> {

	private final Injector context;
	private final ActionExecutor executor;

	public MulticastDispatch(Injector context, ActionExecutor executor) {
		this.context = context;
		this.executor = executor;
	}

	@Override
	public B execute(A input, List<ActionSite<A, B>> sites) {
		ActionExecutionFailed ex = null;
		int disconnected = 0;
		B res = null;
		for (ActionSite<A, B> site : sites) {
			try {
				res = executor.execute(site, site.args(context, input), input);
			} catch (DisconnectException e) {
				// not incrementing the index as element at that index now is the next in line
				disconnected++;
			} catch (ActionExecutionFailed e) {
				ex = e;
			}
		}
		if (sites.size() <= disconnected)
			throw new DisconnectException("All sites disconnected");
		if (res != null)
			return res;
		if (ex != null)
			throw ex;
		return null;
	}
}
