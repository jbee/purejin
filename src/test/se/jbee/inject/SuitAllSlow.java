package se.jbee.inject;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import se.jbee.inject.event.SuitEventSlow;

@RunWith(Suite.class)
@SuiteClasses({ SuiteAllFast.class, SuitEventSlow.class })
public class SuitAllSlow {

}
