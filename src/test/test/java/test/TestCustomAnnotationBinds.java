package test;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ServiceLoader;

import org.junit.Test;

import com.example.app.Support;

import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency.IllegalAcccess;
import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.bind.BinderModuleWith;
import se.jbee.inject.bind.serviceloader.ServiceLoaderAnnotations;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.declare.Bundle;
import se.jbee.inject.declare.ModuleWith;

/**
 * A test that demonstrates how a custom annotation is defined as
 * {@link ModuleWith} (here {@link ServiceAnnotation}), how it is added to the
 * bootstrapping configuration and how to request its use.
 * 
 * Second example shows that custom annotations can also be provided via
 * {@link ServiceLoader} using {@link ModuleWith} as contract. This has the
 * benefit of plugging into the bootstrapping without additional setup code but
 * has the drawback that the class implementing the annotation effect cannot
 * have constructor arguments.
 */
public class TestCustomAnnotationBinds {

	/* Assume following to be defined in some software module... */

	@Target(TYPE)
	@Retention(RUNTIME)
	static @interface Service {

	}

	/**
	 * Applies the effects of the {@link Service} annotation.
	 */
	static class ServiceAnnotation extends BinderModuleWith<Class<?>> {

		@Override
		protected void declare(Class<?> annotated) {
			per(Scope.application).withIndirectAccess() // withIndirectAccess just used as an example (not needed)
					.autobind(annotated).toConstructor();
		}
	}

	/* Assume following to be defined in another software module... */

	static interface SomeService {

	}

	@Service
	static class SomeServiceImpl implements Serializable, SomeService {

	}

	/* Assume following to be defined in another software module... */

	@Support // annotation and its effect is defined in the com.example.app test dependency
	static class SomeOtherService {

	}

	/* Assume following to be defined in same module as above but in its
	 * composition configuration */

	/**
	 * The separate {@link Bundle} definition should just isolate the
	 * {@link ServiceLoader} depended test from the programmatic setup dependent
	 * test.
	 */
	static class TestCustomAnnotationBindsModule1 extends BinderModule {

		@Override
		protected void declare() {
			addAnnotated(SomeServiceImpl.class);
		}

	}

	/**
	 * The separate {@link Bundle} definition should just isolate the
	 * {@link ServiceLoader} depended test from the programmatic setup dependent
	 * test.
	 */
	static class TestCustomAnnotationBindsModule2 extends BinderModule {

		@Override
		protected void declare() {
			addAnnotated(SomeOtherService.class);
		}

	}

	@Test
	public void customAnnotationsAddedProgrammaticallyToGlobals() {
		Env env = Bootstrap.ENV.withAnnotation(Service.class,
				new ServiceAnnotation());
		Injector injector = Bootstrap.injector(env,
				TestCustomAnnotationBindsModule1.class);
		SomeService service = injector.resolve(SomeService.class);
		assertNotNull(service);
		assertSame(SomeServiceImpl.class, service.getClass());
		assertSame(SomeServiceImpl.class,
				injector.resolve(Serializable.class).getClass());
		// just to check the access limitation is honoured
		try {
			injector.resolve(SomeServiceImpl.class);
			fail("Expected interface must be used");
		} catch (IllegalAcccess e) {
		}
	}

	/**
	 * This verifies that the {@link Support} annotation's effect is loaded via
	 * {@link ServiceLoader}. It is defined in the com.example.app test
	 * dependency jar file. If the {@link SomeOtherService} can be resolved it
	 * was bound which meant the annotation had done its effect. Otherwise no
	 * binding would exist for the class.
	 */
	@Test
	public void customAnnotationsAddedViaServiceLoader() {
		Env env = Bootstrap.env(ServiceLoaderAnnotations.class);
		Injector injector = Bootstrap.injector(env,
				TestCustomAnnotationBindsModule2.class);
		SomeOtherService service = injector.resolve(SomeOtherService.class);
		assertNotNull(service);
	}
}
