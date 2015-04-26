package se.jbee.inject.bind;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith ( Suite.class )
@SuiteClasses ( { TestAutobindBinds.class, TestElementBinds.class, TestMultibindBinds.class,
		TestConstantBinds.class, TestSupplierBinds.class, TestInstanceBinds.class,
		TestPackageLocalisedBinds.class, TestEditionFeatureBinds.class, TestModularBinds.class,
		TestTargetedBinds.class, TestLoggerBinds.class, TestRobotLegsProblemBinds.class,
		TestConstructorParameterBinds.class, TestDependencyParameterBinds.class,
		TestScopedBinds.class, TestInjectronBinds.class, TestProviderBinds.class,
		TestPrimitiveBinds.class, TestInjectorExceptions.class, TestCollectionBinds.class,
		TestInspectorBinds.class, TestParentTargetBinds.class, TestPresetModuleBinds.class,
		TestRequiredProvidedBinds.class, TestStateDependentBinds.class,
		TestPrimitiveArrayBinds.class, TestMultipleOptionChoicesBinds.class, TestMacroBinds.class,
		TestBootstrapper.class, TestLinker.class, TestIssue1.class, TestDecoratorBinds.class, 
		TestBinderModule.class, TestExample1Binds.class, TestPluginBinds.class } )
public class SuiteBind {
	// all tests in the bind package
}
