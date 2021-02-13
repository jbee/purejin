package se.jbee.junit.assertion;

import java.time.Duration;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.time.Duration.ofMillis;

public final class Await {

	public static Await atMost(Duration maxWaitTime) {
		return new Await(Duration.ZERO, maxWaitTime, Duration.ZERO);
	}

	private final Duration pollInterval;
	public final Duration maxWaitTime;
	public final Duration minWaitTime;

	private Await(Duration pollInterval, Duration maxWaitTime,
			Duration minWaitTime) {
		this.pollInterval = pollInterval;
		this.maxWaitTime = maxWaitTime;
		this.minWaitTime = minWaitTime;
	}

	public Await tryInInterval(Duration pollInterval) {
		return new Await(pollInterval, maxWaitTime, minWaitTime);
	}

	public Await atLeast(Duration minWaitTime) {
		return new Await(pollInterval, maxWaitTime, minWaitTime);
	}

	public Duration pollInterval() {
		if (!pollInterval.isZero())
			return pollInterval;
		long maxWaitMillis = maxWaitTime.toMillis();
		return ofMillis(max(1, min(100, maxWaitMillis / 10)));
	}
}
