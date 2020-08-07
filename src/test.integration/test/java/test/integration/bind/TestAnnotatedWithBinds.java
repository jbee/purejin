package test.integration.bind;

import org.junit.Test;
import se.jbee.inject.Annotated;
import se.jbee.inject.AnnotatedWith;
import se.jbee.inject.AnnotatedWith.AnnotatedInstance;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Environment;

import java.lang.annotation.*;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static se.jbee.inject.config.ProducesBy.declaredMethods;
import static se.jbee.inject.config.SharesBy.declaredFields;

public class TestAnnotatedWithBinds {

	@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@interface MXBean {

	}

	@MXBean
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

	@MXBean
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

	public static class TestAnnotatedWithBindsModule extends BinderModule {

		@Marker
		public final String fieldResource = "foo";

		@Override
		protected void declare() {
			bind(Bean.class).toConstructor();
			bind(Service.class).to(ServiceImpl.class);
			autobind() //
					.produceBy(declaredMethods.annotatedWith(Marker.class)) //
					.shareBy(declaredFields.annotatedWith(Marker.class)) //
					.in(this);
		}

		@Marker
		public int methodResource() {
			return 42;
		}
	}

	private final Injector injector = Bootstrap.injector(
			Environment.DEFAULT.with(Annotated.Merge.class,
					TestAnnotatedWithBinds::advancedAnnotations),
			TestAnnotatedWithBindsModule.class);

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
	public void annotatedInstancesAreInjected() {
		injector.resolve(Service.class).verify();
	}

	@Test
	public void annotatedInstancesCanBeResolvedProgrammatically() {
		List<AnnotatedInstance<?>> annotated = injector.annotatedWith(
				MXBean.class);
		assertEquals(1, annotated.size());
		assertSame(injector.resolve(Service.class),
				annotated.get(0).instance.get());
		assertEquals(ServiceImpl.class, annotated.get(0).role);
	}

	@Test
	public void annotatedInstancesCanOriginateFromMethodsAndFields() {
		List<AnnotatedInstance<?>> annotated = injector.annotatedWith(
				Marker.class);
		assertEquals(2, annotated.size());
		assertEquals(new HashSet<>(asList(42, "foo")),
				new HashSet<>(asList(annotated.get(0).instance.get(),
						annotated.get(1).instance.get())));
	}

	@Test
	public void annotatedInstancesListIsCached() {
		assertSame(injector.annotatedWith(Marker.class),
				injector.annotatedWith(Marker.class));
	}
}
