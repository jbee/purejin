package se.jbee.inject.action;

import se.jbee.inject.DisconnectException;
import se.jbee.inject.Injector;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@link RoundRobinStrategy} tries to use all available {@link ActionSite}
 * equally often.
 * <p>
 * This basic implementation does not have any particular guarantees of fairness
 * in the presence of a changing list due to dynamic connects and disconnects.
 */
public final class RoundRobinStrategy<A, B> implements ActionStrategy<A, B> {

	private final Injector context;
	private final Executor executor;
	private final AtomicInteger callCount = new AtomicInteger();

	public RoundRobinStrategy(Injector context,	Executor executor) {
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