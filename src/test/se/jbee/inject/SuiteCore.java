package se.jbee.inject;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestName.class, TestType.class, TestPackages.class,
		TestMoreApplicable.class, TestTarget.class, TestDeclarationType.class,
		TestEqualsHashCode.class, TestSerializable.class,
		TestTypeVariable.class })
public class SuiteCore {

}
