package se.jbee.inject.bind;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestAutobindBinds.class, TestElementBinds.class,
		TestMultibindBinds.class, TestConstantBinds.class,
		TestSupplierBinds.class, TestInstanceBinds.class,
		TestPackageLocalisedBinds.class, TestEditionFeatureBinds.class,
		TestToggledBinds.class, TestTargetedBinds.class, TestLoggerBinds.class,
		TestRobotLegsProblemBinds.class, TestConstructorParameterBinds.class,
		TestDependencyParameterBinds.class, TestScopedBinds.class,
		TestResolveResourceBinds.class, TestProviderBinds.class,
		TestPrimitiveBinds.class, TestInjectorExceptions.class,
		TestCollectionBinds.class, TestMirrorAutobindBinds.class,
		TestParentTargetBinds.class, TestModuleWithBinds.class,
		TestRequiredProvidedBinds.class, TestStateDependentBinds.class,
		TestPrimitiveArrayBinds.class, TestMultipleChoicesBinds.class,
		TestMacroBinds.class, TestBootstrapper.class, TestLinker.class,
		TestIssue1.class, TestDecoratorBinds.class, TestBinderModule.class,
		TestExample1Binds.class, TestPluginBinds.class, TestMockingBinds.class,
		TestLambdaBinds.class, TestInitialiserBinds.class,
		TestEditionPackageBinds.class, TestPubSubBinds.class,
		TestSetterInitialisationBinds.class, TestDynamicInitialiserBinds.class,
		TestMacroBinds.class, TestGeneratorBinds.class,
		TestDiskScopeBinds.class, TestConfigBinds.class,
		TestInitialiserDecorationBinds.class,
		TestInitialiserAnnotationBinds.class, TestPropertyAnnotationBinds.class,
		TestInjectorHierarchy.class, TestAnnotatedWithBinds.class,
		TestYieldListeners.class, TestCustomAnnotationBinds.class,
		TestArrayBinds.class, TestIndirectBinds.class,
		TestInstallInSubContextBinds.class, TestDefaultMacroBinds.class,
		TestInjectorDecorationBinds.class })
public class SuiteBind {
	// all tests in the bind package
}
