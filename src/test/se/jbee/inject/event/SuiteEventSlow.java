package se.jbee.inject.event;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestNonConcurrentVoidMultiDispatchEvents.class,
		TestTTLExceptionHandingComputeEvents.class })
public class SuiteEventSlow {

}
