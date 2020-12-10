package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Initialiser;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import test.integration.util.Resource;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Name.named;

/**
 * This test demonstrates how a {@link Initialiser} for {@link Object} can be
 * used to add annotation based injection to the {@link Injector}.
 *
 * In this basic example the {@link Resource} annotation is used to mark field
 * that should be injected. The naive implementation of the annotation
 * {@link Initialiser} then iterates the {@link Field}s of the target to check
 * for the annotation and inject the fields resolving them from the
 * {@link Injector} context. A serious implementation obviously has to be more
 * sophisticated in how to identify and initialised fields based on annotations
 * found but the principle of using the {@link Initialiser} to add this feature
 * stays the same.
 */
class TestInitialiserAnnotationBinds {

	static final class TestInitialiserAnnotationBindsModule extends BinderModule
			implements Initialiser<Object> {

		@Override
		protected void declare() {
			bind(Integer.class).to(42);
			bind(String.class).to("y");
			bind(named("x"), String.class).to("x");
			construct(SomeBean.class);
			initbind(Object.class).to(this);
		}

		@Override
		public Object init(Object target, Injector context) {
			for (Field f : target.getClass().getDeclaredFields()) {
				if (f.isAnnotationPresent(Resource.class)) {
					try {
						Object value = context.resolve(
								f.getAnnotation(Resource.class).value(),
								f.getType());
						f.set(target, value);
					} catch (Exception e) {
						throw new AssertionError(
								"failed to inject annotated field", e);
					}
				}
			}
			return target;
		}

	}

	public static class SomeBean {

		@Resource
		Integer someField;

		@Resource("x")
		String someNamedField;
	}

	private final Injector injector = Bootstrap.injector(
			TestInitialiserAnnotationBindsModule.class);

	@Test
	void initialisiersCanBeUsedToAddAnnotationBasedInjection() {
		SomeBean bean = injector.resolve(SomeBean.class);
		assertEquals(42, bean.someField.intValue());
		assertEquals("x", bean.someNamedField);
	}
}
