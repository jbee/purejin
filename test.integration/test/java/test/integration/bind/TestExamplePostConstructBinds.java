package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Produces;
import se.jbee.inject.binder.Supply;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.HintsBy;
import se.jbee.inject.config.ProducesBy;
import se.jbee.lang.Type;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.lang.Type.classType;

/**
 * This example shows the effect of the java enterprise {@code PostConstruct}
 * annotation can be added.
 * <p>
 * In this example we use a copy of that annotation as it isn't on the module
 * path. The @{@link PostConstruct} of this example will also allow for the
 * annotated methods to have parameters in which case the {@link Injector}
 * context will try to inject them.
 * <p>
 * This example also shows how a similar concept can be build more elegantly
 * using an interface.
 *
 * @see TestExampleLiftAnnotationGuidedInjectionBinds
 * @see TestExamplePubSubBinds
 */
class TestExamplePostConstructBinds {

	/**
	 * A "clone" of the java enterprise annotation {@code PostConstruct}.
	 */
	@Retention(value= RUNTIME)
	@Target(value= METHOD)
	@interface PostConstruct {}

	@FunctionalInterface
	interface IPostConstruct {

		void setUp();
	}

	private static final class TestExamplePostConstructBindsModule extends
			BinderModule {

		@Override
		protected void declare() {
			construct(Bean.class);
			bind(int.class).to(7);
			injectingInto(Bean.class).bind(int.class).to(42);

			// @PostConstruct
			lift(Object.class).to((TestExamplePostConstructBindsModule::postConstructHook));

			// IPostConstruct (name just used to avoid name clash in this example)
			lift(IPostConstruct.class).run(IPostConstruct::setUp);
		}

		private static Object postConstructHook(Object target, Type<?> as,
				Injector context) {
			// we use the ProducesBy util to find the methods
			Method[] postConstructs = ProducesBy.declaredMethods(false) //
					.annotatedWith(PostConstruct.class) //
					.reflect(target.getClass());
			if (postConstructs == null)
				return target;
			// each found method is called
			for (Method m : postConstructs)
				call(target, m, context);
			return target;
		}

		@SuppressWarnings("unchecked")
		private static <T> void call(Object target, Method m, Injector context) {
			// to call the method we use the Produces and Supply utilities
			// which means we also support injecting parameters into these
			// method calls
			Produces<T> prod = (Produces<T>) Produces.produces(target, m,
					context.resolve(Env.class)
							.in(m.getDeclaringClass())
							.property(HintsBy.class, HintsBy.AUTO));
			Supply.byProduction(prod).supply(dependency(prod.actualType) //
					// adds basic targeting so that injectingInto is respected when method arguments are injected
					// to get fully correct target context a Supplier<Lift<Object>> would be needed instead of postConstructHook (Lift<Object>) so that the actual Dependency can be accessed
					.injectingInto(classType(target.getClass())),
					context);
		}
	}

	public static class Bean implements IPostConstruct {

		String callMe = "not called";
		int callMeWith = 0;

		String setUp = "not called";

		@PostConstruct
		public void callMe() {
			callMe = "called";
		}

		@PostConstruct
		public void callMeWith(Integer value) {
			this.callMeWith = value;
		}

		@Override
		public void setUp() {
			setUp = "called";
		}
	}

	private final Injector context = Bootstrap.injector(
			TestExamplePostConstructBindsModule.class);

	@Test
	void postConstructAnnotatedMethodVoidNoParameters() {
		assertEquals("called", context.resolve(Bean.class).callMe);
	}

	@Test
	void postConstructAnnotatedMethodVoidInjectedParameters() {
		assertEquals(42, context.resolve(Bean.class).callMeWith);
	}

	@Test
	void postConstructInterfaceMethod() {
		assertEquals("called", context.resolve(Bean.class).setUp);
	}
}
