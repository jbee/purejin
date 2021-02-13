package test.integration.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import se.jbee.inject.*;
import se.jbee.inject.bind.BindingConsolidation;
import se.jbee.inject.binder.Accesses;
import se.jbee.inject.binder.Constant;
import se.jbee.inject.binder.Constructs;
import se.jbee.inject.binder.Produces;
import se.jbee.inject.config.*;
import se.jbee.inject.defaults.DefaultEnv;
import se.jbee.lang.Type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.jbee.inject.bind.ValueBinder.valueBinderTypeOf;

/**
 * Tests the bootstrapping process using the {@link DefaultEnv} as the first
 * {@link Env} so that further {@link Env} and {@link se.jbee.inject.Injector}s
 * can be bootstrapped.
 */
class TestDefaultEnv {

	@Test
	void bootstrappingDoesNotThrow() {
		assertDoesNotThrow((ThrowingSupplier<Env>) DefaultEnv::bootstrap);
	}

	@Test
	void defaultEnvDefinesContainerLifeCycle() {
		Env env = DefaultEnv.bootstrap();
		assertDefined(env, Scope.container.toString(), ScopeLifeCycle.class);
	}

	@Test
	void defaultEnvDefinesValueBinders() {
		Env env = DefaultEnv.bootstrap();
		assertDefined(env, valueBinderTypeOf(Constructs.class));
		assertDefined(env, valueBinderTypeOf(Constant.class));
		assertDefined(env, valueBinderTypeOf(Produces.class));
		assertDefined(env, valueBinderTypeOf(Accesses.class));
		assertDefined(env, valueBinderTypeOf(Instance.class));
		assertDefined(env, valueBinderTypeOf(Descriptor.BridgeDescriptor.class));
		assertDefined(env, valueBinderTypeOf(Descriptor.ArrayDescriptor.class));
	}

	@Test
	void defaultEnvDefinesStrategies() {
		Env env = DefaultEnv.bootstrap();
		assertDefined(env, ConstructsBy.class);
		assertDefined(env, AccessesBy.class);
		assertDefined(env, ProducesBy.class);
		assertDefined(env, NamesBy.class);
		assertDefined(env, ScopesBy.class);
		assertDefined(env, HintsBy.class);
		assertDefined(env, PublishesBy.class);
	}

	@Test
	void defaultEnvDefinesReflection() {
		Env env = DefaultEnv.bootstrap();
		assertDefined(env, New.class);
		assertDefined(env, Invoke.class);
		assertDefined(env, Get.class);
	}

	@Test
	void defaultEnvDefinesBindingConsolidation() {
		assertDefined(DefaultEnv.bootstrap(), BindingConsolidation.class);
	}

	@Test
	void defaultEnvDefinesDefaultSettings() {
		Env env = DefaultEnv.bootstrap();
		assertDefined(env, Env.USE_VERIFICATION, boolean.class);
	}

	private static void assertDefined(Env env, Class<?> property) {
		assertDefined(env, Type.raw(property));
	}

	private static void assertDefined(Env env, Type<?> property) {
		assertDoesNotThrow(() -> assertNotNull(
				env.property(property)));
	}

	private static void assertDefined(Env env, String qualifier, Class<?> property) {
		assertDefined(env, qualifier, Type.raw(property));
	}

	private static void assertDefined(Env env, String qualifier, Type<?> property) {
		assertDoesNotThrow(() -> assertNotNull(
				env.property(qualifier, property)));
	}
}
