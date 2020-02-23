package se.jbee.inject.bind;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashSet;

import javax.management.MXBean;

import org.junit.Test;

import se.jbee.inject.AnnotatedWith;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.Plugins;

/**
 * A basic test that demonstrates how type level annotations become
 * {@link Plugins} where the {@link Annotation} type is the plug-in point and
 * the annotated class the plug-in.
 * 
 * This allows to resolve e.g all instances annotated with a certain annotation
 * by first resolving their {@link Plugins} point and using the resolved array
 * of {@link Class}es to resolve each of them.
 * 
 * @author Jan Bernitt
 */
public class TestAnnotatedWithBinds {

	@MXBean
	static interface Service {

	}

	@Target(TYPE)
	@Retention(RUNTIME)
	static @interface WebService {

	}

	@Target(TYPE)
	@Retention(RUNTIME)
	static @interface Resource {

	}

	@Resource
	static class ServiceImpl implements Service {

		final AnnotatedWith<WebService> webServices;

		public ServiceImpl(AnnotatedWith<WebService> webServices) {
			this.webServices = webServices;
		}
	}

	@WebService
	@Resource
	static class Bean {

	}

	static class TestAnnotatedWithBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(Bean.class).toConstructor();
			bind(Service.class).to(ServiceImpl.class);
		}

	}

	private Injector injector = Bootstrap.injector(
			TestAnnotatedWithBindsModule.class);

	@Test
	public void annotationsOnToTypesWorkAsPluginPoints() {
		Plugins plugins = injector.resolve(Plugins.class);
		assertEqualSets(new Class[] { Bean.class, ServiceImpl.class },
				plugins.forPoint(Resource.class));
		assertEqualSets(new Class[] { Bean.class },
				plugins.forPoint(WebService.class));
	}

	@Test
	public void annotationsOnResourceTypesWorkAsPluginPoints() {
		Plugins plugins = injector.resolve(Plugins.class);
		assertEqualSets(new Class[] { Service.class },
				plugins.forPoint(MXBean.class));
	}

	@Test
	public void annotatedWithAllowsInjectionOfAnnotatedToTypeInstances() {
		ServiceImpl service = injector.resolve(ServiceImpl.class);
		assertNotNull(service);
		assertEquals(Bean.class,
				service.webServices.annotated().iterator().next().getClass());
	}

	@Test
	public void annotatedWithAllowsInjectionOfAnnotatedResourceTypeInstances() {
		assertEquals(injector.resolve(ServiceImpl.class),
				injector.annotatedWith(MXBean.class).iterator().next());
	}

	private static <T> void assertEqualSets(T[] expected, T[] actual) {
		assertEquals(new HashSet<>(asList(expected)),
				new HashSet<>(asList(actual)));
	}
}
