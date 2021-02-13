package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.UnresolvableDependency.IllegalAccess;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.ModuleWith;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BinderModuleWith;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.PublishesBy;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ServiceLoader;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.raw;

/**
 * This test demonstrates how {@link se.jbee.inject.binder.Binder#detectAt(Class[])}
 * in combination with user {@link java.lang.annotation.Annotation}s can be used
 * to define your own concepts. The effect of such a concept is defined as
 * {@link ModuleWith} (here {@link ServiceAnnotationTemplet}) that is added to
 * the {@link Env} which links the user {@link java.lang.annotation.Annotation}
 * and its effect.
 * <p>
 * With such a pattern present in the {@link Env} annotated classes are now
 * simply added using {@link se.jbee.inject.binder.Binder#detectAt(Class[])}.
 * The effects of this follow the pattern as implemented in {@link
 * ServiceAnnotationTemplet} which receives the annotated {@link Class} as an
 * argument.
 *
 * @see TestExampleConfigPropertyAnnotationBinds
 **/
class TestExampleAnnotationGuidedBindingBinds {

	/* Assume following to be defined in some user software module... */

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

	@Target(METHOD)
	@Retention(RUNTIME)
	@interface Provides {
	}

	/* Assume following added as general configuration in a core module... */

	/**
	 * Applies the effects of the {@link Service} annotation.
	 */
	static class ServiceAnnotationTemplet extends BinderModuleWith<Class<?>> {

		@Override
		protected void declare(Class<?> annotated) {
			per(Scope.application)
					.withIndirectAccess() // withIndirectAccess just used as an example (not needed)
					.withPublishedAccess().bind(annotated).toConstructor();
		}
	}

	/**
	 * Applies the effect of the {@link Contract} annotation which binds the
	 * class for all named interfaces.
	 */
	static class ContractAnnotationTemplet extends BinderModuleWith<Class<?>> {

		@Override
		protected void declare(Class<?> annotated) {
			for (Class<?> api : annotated.getAnnotation(Contract.class).value()) {
				declareContract(api, annotated);
			}
			withIndirectAccess().bind(annotated).toConstructor();
		}

		@SuppressWarnings("unchecked")
		private <T> void declareContract(Class<T> api, Class<?> impl) {
			if (!raw(impl).isAssignableTo(raw(api))) {
				fail(api + " in not implemented by "+ impl);
			} else {
				bind(api).to((Class<? extends T>) impl);
			}
		}
	}

	static class ProvidesAnnotationTemplet extends BinderModuleWith<Method> {

		@Override
		protected void declare(Method annotated) {
			implicit().bind(annotated.getDeclaringClass()).toConstructor();
			autobind().asProducer(annotated, null);
		}
	}

	/* Assume following to be defined in other user software module... */

	interface SomeService {

	}

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

	public static class Bean {
		@Provides
		public long methodAnnotation() {
			return 13L;
		}
	}

	/**
	 * The separate {@link Bundle} definition should just isolate the
	 * {@link ServiceLoader} depended test from the programmatic setup dependent
	 * test.
	 */
	private static class TestExampleAnnotationGuidedBindingBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			detectAt(SomeServiceImpl.class);
			detectAt(Answer.class);
			detectAt(Bean.class);
		}
	}

	private final Env env = Bootstrap.DEFAULT_ENV //
			.with(PublishesBy.class, PublishesBy.OPTIMISTIC)
			.with(named(Service.class), ModuleWith.TYPE_ANNOTATION, new ServiceAnnotationTemplet())
			.with(named(Contract.class), ModuleWith.TYPE_ANNOTATION, new ContractAnnotationTemplet())
			.with(named(Provides.class), ModuleWith.METHOD_ANNOTATION, new ProvidesAnnotationTemplet());

	private final Injector context = Bootstrap.injector(env,
			TestExampleAnnotationGuidedBindingBindsModule.class);

	@Test
	void customAnnotationsAddedProgrammatically() {
		SomeService service = context.resolve(SomeService.class);
		assertNotNull(service);
		assertSame(SomeServiceImpl.class, service.getClass());
		assertSame(SomeServiceImpl.class,
				context.resolve(Serializable.class).getClass());
		// just to check the access limitation is honoured
		assertThrows(IllegalAccess.class,
				() -> context.resolve(SomeServiceImpl.class));
	}

	@Test
	void customAnnotationsWithPropertiesAddedProgrammatically() {
		IntSupplier answer = context.resolve(IntSupplier.class);
		assertNotNull(answer);
		assertEquals(42, answer.getAsInt());
		// in contrast to @Service the @Contract only binds the named interfaces, so...
		assertThrows(UnresolvableDependency.ResourceResolutionFailed.class,
				() -> context.resolve(LongSupplier.class));
	}

	@Test
	void customMethodAnnotationsAddedProgrammatically() {
		// Bean has a method annotated @Provides which returns 13L
		// this is bound for long as an effect of the ProvidesAnnotationEffect
		assertEquals(13, context.resolve(long.class));
	}
}
