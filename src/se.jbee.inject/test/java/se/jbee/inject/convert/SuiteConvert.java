package se.jbee.inject.convert;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestChain.class, TestCollectionConverter.class,
		TestConverter.class, TestImported.class })
public class SuiteConvert {

}
