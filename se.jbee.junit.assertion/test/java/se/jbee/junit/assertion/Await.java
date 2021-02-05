package se.jbee.junit.assertion;

import java.time.Duration;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.time.Duration.ofMillis;

public final class Await {

	private static final Await DEFAULT = new Await(ofMillis(5), ofMillis(100),
			Duration.ZERO);

	public static Await await() {
		return DEFAULT;
	}

	private final Duration pollInterval;
	public final Duration maxWaitTime;
	public final Duration minWaitTime;

	public Await(Duration pollInterval, Duration maxWaitTime,
			Duration minWaitTime) {
		this.pollInterval = pollInterval;
		this.maxWaitTime = maxWaitTime;
		this.minWaitTime = minWaitTime;
	}

	public Await inInterval(Duration pollInterval) {
		return new Await(pollInterval, maxWaitTime, minWaitTime);
	}

	public Await atMost(Duration maxWaitTime) {
		return new Await(pollInterval, maxWaitTime, minWaitTime);
	}

	public Await atLeast(Duration minWaitTime) {
		return new Await(pollInterval, maxWaitTime, minWaitTime);
	}

	public Duration pollInterval() {
		if (pollInterval != null)
			return pollInterval;
		long maxWaitMillis = maxWaitTime.toMillis();
		return ofMillis(max(1, min(100, maxWaitMillis / 10)));
	}
}
