package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.ModuleWith;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BinderModuleWith;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Environment;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ServiceLoader;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.lang.Type.raw;

/**
 * A test that demonstrates how a custom annotation is defined as
 * {@link ModuleWith} (here {@link ServiceAnnotation}), how it is added to the
 * bootstrapping configuration and how to request its use.
 **/
public class TestAddAnnotatedBinds {

	/* Assume following to be defined in some software module... */

	@Target(TYPE)
	@Retention(RUNTIME)
	@interface Service {
	}

	@Target(TYPE)
	@Retention(RUNTIME)
	@interface Contract {
		/**
		 * This example shows that the annotations can also have properties
		 * which are used when applying the annotation effect.
		 */
		Class<?>[] value();
	}

	/**
	 * Applies the effects of the {@link Service} annotation.
	 */
	static class ServiceAnnotation extends BinderModuleWith<Class<?>> {

		@Override
		protected void declare(Class<?> annotated) {
			per(Scope.application)
					.withIndirectAccess() // withIndirectAccess just used as an example (not needed)
					.autobind(annotated).toConstructor();
		}
	}

	/**
	 * Applies the effect of the {@link Contract} annotation which binds the
	 * class for all named interfaces.
	 */
	static class ContractAnnotation extends BinderModuleWith<Class<?>> {

		@Override
		protected void declare(Class<?> annotated) {
			for (Class<?> api : annotated.getAnnotation(Contract.class).value()) {
				declareContract(api, annotated);
			}
			withIndirectAccess().bind(annotated).toConstructor();
		}

		private <T> void declareContract(Class<T> api, Class<?> impl) {
			if (!raw(impl).isAssignableTo(raw(api))) {
				fail(api + " in not implemented by "+ impl);
			} else {
				bind(api).to((Class<? extends T>) impl);
			}
		}
	}

	/* Assume following to be defined in another software module... */

	interface SomeService {

	}
	/* Assume following to be defined in another software module... */

	@Service
	public static class SomeServiceImpl implements Serializable, SomeService {

	}

	@Contract(IntSupplier.class)
	public static class Answer implements IntSupplier, LongSupplier {

		@Override
		public int getAsInt() {
			return 42;
		}

		@Override
		public long getAsLong() {
			return -1;
		}
	}



	/* Assume following to be defined in same module as above but in its
	 * composition configuration */

	/**
	 * The separate {@link Bundle} definition should just isolate the
	 * {@link ServiceLoader} depended test from the programmatic setup dependent
	 * test.
	 */
	static class TestAddAnnotatedBindsModule extends BinderModule {

		@Override
		protected void declare() {
			addAnnotated(SomeServiceImpl.class);
			addAnnotated(Answer.class);
		}
	}

	private final Env env = Environment.DEFAULT //
			.withTypeAnnotation(Service.class, new ServiceAnnotation())
			.withTypeAnnotation(Contract.class, new ContractAnnotation());
	private final Injector injector = Bootstrap.injector(env,
			TestAddAnnotatedBindsModule.class);

	@Test
	public void customAnnotationsAddedProgrammatically() {
		SomeService service = injector.resolve(SomeService.class);
		assertNotNull(service);
		assertSame(SomeServiceImpl.class, service.getClass());
		assertSame(SomeServiceImpl.class,
				injector.resolve(Serializable.class).getClass());
		// just to check the access limitation is honoured
		assertThrows(UnresolvableDependency.IllegalAccess.class,
				() -> injector.resolve(SomeServiceImpl.class));
	}

	@Test
	public void customAnnotationsWithPropertiesAddedProgrammatically() {
		IntSupplier answer = injector.resolve(IntSupplier.class);
		assertNotNull(answer);
		assertEquals(42, answer.getAsInt());
		// in contrast to @Service the @Contract only binds the named interfaces, so...
		assertThrows(UnresolvableDependency.NoResourceForDependency.class,
				() -> injector.resolve(LongSupplier.class));
	}
}
