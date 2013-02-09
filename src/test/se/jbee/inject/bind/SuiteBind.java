package se.jbee.inject.bind;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith ( Suite.class )
@SuiteClasses ( { TestAutobindBinds.class, TestElementBinds.class, TestInstanceBinds.class,
		TestServiceMethodBinds.class, TestServiceBinds.class, TestCommandBinds.class,
		TestSupplierBinds.class, TestServiceInspectorBinds.class, TestTypeBinds.class,
		TestBootstrapper.class, TestPackageLocalisedBinds.class, TestEditionFeatureBinds.class,
		TestModularBinds.class, TestTargetedBinds.class, TestLoggerBinds.class,
		TestRobotLegsProblemBinds.class, TestConstructorParameterBinds.class,
		TestDependencyParameterBinds.class, TestScopedBinds.class, TestExtensionBinds.class,
		TestInjectronBinds.class, TestProviderBinds.class, TestServiceInvocationBinds.class,
		TestPrimitiveBinds.class, TestInjectorExceptions.class, TestCollectionBinds.class,
		TestInspectorBinds.class, TestParentTargetBinds.class, TestPresetModuleBinds.class,
		TestLinker.class } )
public class SuiteBind {
	// all tests in the bind package
}
