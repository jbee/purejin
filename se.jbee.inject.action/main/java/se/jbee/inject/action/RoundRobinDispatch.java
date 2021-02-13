package se.jbee.inject.action;

import se.jbee.inject.DisconnectException;
import se.jbee.inject.Injector;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@link RoundRobinDispatch} tries to use all available {@link ActionSite}
 * equally often.
 * <p>
 * This basic implementation does not have any particular guarantees of fairness
 * in the presence of a changing list due to dynamic connects and disconnects.
 */
public final class RoundRobinDispatch<A, B> implements
		ActionDispatch<A, B> {

	private final Injector context;
	private final ActionExecutor executor;
	private final AtomicInteger callCount = new AtomicInteger();

	public RoundRobinDispatch(Injector context,	ActionExecutor executor) {
		this.context = context;
		this.executor = executor;
	}

	@Override
	public B execute(A input, List<ActionSite<A, B>> sites) {
		int disconnected = 0;
		while (disconnected < sites.size()) {
			int i = callCount.getAndIncrement();
			ActionSite<A, B> site = sites.get(i % sites.size());
			try {
				return executor.execute(site, site.args(context, input), input);
			} catch (DisconnectException ex) {
				disconnected++; // test the next
			}
		}
		throw new DisconnectException("All sites disconnected");
	}
}
