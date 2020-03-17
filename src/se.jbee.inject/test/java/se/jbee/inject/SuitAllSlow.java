package se.jbee.inject;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import se.jbee.inject.event.SuiteEventSlow;

@RunWith(Suite.class)
@SuiteClasses({ SuiteAllFast.class, SuiteEventSlow.class })
public class SuitAllSlow {

}
