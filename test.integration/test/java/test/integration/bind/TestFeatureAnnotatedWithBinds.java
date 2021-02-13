package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Annotated;
import se.jbee.inject.AnnotatedWith;
import se.jbee.inject.AnnotatedWith.AnnotatedInstance;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.AccessesBy;

import java.lang.annotation.*;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static se.jbee.inject.config.ProducesBy.OPTIMISTIC;
import static se.jbee.lang.Type.raw;

class TestFeatureAnnotatedWithBinds {

	@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@interface Component {

	}

	@Component
	@FunctionalInterface
	interface Service {

		void verify();
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface WebService {

		String value();
	}

	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface Marker {

	}

	@Component
	public static class ServiceImpl implements Service {

		final AnnotatedWith<WebService> webServices;
		final Bean bean;

		public ServiceImpl(AnnotatedWith<WebService> webServices, Bean bean) {
			this.webServices = webServices;
			this.bean = bean;
		}

		@Override
		public void verify() {
			assertEquals(1, webServices.instances().size());
			AnnotatedInstance<?> instance = webServices.instances().get(0);
			assertSame(bean, instance.instance.get());
			assertEquals("foo", instance.annotations.getAnnotation(
					WebService.class).value());
		}
	}

	@WebService("foo")
	public static class Bean {

	}

	public static class TestFeatureAnnotatedWithBindsModule
			extends BinderModule {

		@Marker
		public final String fieldResource = "foo";

		@Override
		protected void declare() {
			bind(Bean.class).toConstructor();
			bind(Service.class).to(ServiceImpl.class);
			autobind() //
					.produceBy(OPTIMISTIC.annotatedWith(Marker.class)) //
					.accessBy(AccessesBy.OPTIMISTIC.annotatedWith(Marker.class)) //
					.in(this);
		}

		@Marker
		public int methodResource() {
			return 42;
		}
	}

	private final Injector injector = Bootstrap.injector(
			Bootstrap.DEFAULT_ENV.with(Annotated.Enhancer.class,
					TestFeatureAnnotatedWithBinds::advancedAnnotations),
			TestFeatureAnnotatedWithBindsModule.class);

	/**
	 * This is installed in the {@link Env} to also recognise {@link Class}
	 * level annotations for bound {@link Constructor}s as it is the case for
	 * {@link Bean}.
	 */
	static Annotated advancedAnnotations(Annotated annotated) {
		if (annotated.element() instanceof Constructor) {
			Constructor<?> target = (Constructor<?>) annotated.element();
			return () -> new AnnotatedElement() {

				@Override
				public Annotation[] getDeclaredAnnotations() {
					return target.getDeclaredAnnotations();
				}

				@Override
				public Annotation[] getAnnotations() {
					return target.getAnnotations();
				}

				@Override
				public <T extends Annotation> T getAnnotation(Class<T> type) {
					T annotation = target.getAnnotation(type);
					return annotation != null
						? annotation
						: target.getDeclaringClass().getAnnotation(type);
				}
			};
		}
		return annotated;
	}

	@Test
	void annotatedInstancesAreInjected() {
		injector.resolve(Service.class).verify();
	}

	@Test
	void annotatedInstancesCanBeResolvedProgrammatically() {
		List<AnnotatedInstance<?>> annotated = injector.annotatedWith(
				Component.class);
		assertEquals(1, annotated.size());
		assertSame(injector.resolve(Service.class),
				annotated.get(0).instance.get());
		assertEquals(raw(ServiceImpl.class), annotated.get(0).as);
	}

	@Test
	void annotatedInstancesCanOriginateFromMethodsAndFields() {
		List<AnnotatedInstance<?>> annotated = injector.annotatedWith(
				Marker.class);
		assertEquals(2, annotated.size());
		assertEquals(new HashSet<>(asList(42, "foo")),
				new HashSet<>(asList(annotated.get(0).instance.get(),
						annotated.get(1).instance.get())));
	}

	@Test
	void annotatedInstancesListIsCached() {
		assertSame(injector.annotatedWith(Marker.class),
				injector.annotatedWith(Marker.class));
	}
}
