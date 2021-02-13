package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Installs;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.Config;
import se.jbee.inject.config.HintsBy;
import se.jbee.inject.config.NamesBy;
import se.jbee.lang.Type;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.NoSuchElementException;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.parameterType;
import static se.jbee.lang.Type.raw;

/**
 * A test that show how to add an annotation (here {@link ConfigProperty} that
 * will inject the {@link se.jbee.inject.config.Config} value of the same name
 * to the annotated {@link java.lang.reflect.Parameter}.
 *
 * @see TestExampleAnnotationGuidedBindingBinds
 */
class TestExampleConfigPropertyAnnotationBinds {

	@Documented
	@Target(PARAMETER)
	@Retention(RUNTIME)
	@interface ConfigProperty {

		/**
		 * @return name of the property
		 */
		String value();

		/**
		 * @return the type the property value was defined as
		 */
		Class<?> from() default String.class;
	}

	public static class Bean {

		final String a;
		final String b;

		public Bean(@ConfigProperty("a") String a,
				@ConfigProperty(value = "b", from = Integer.class) String b) {
			this.a = a;
			this.b = b;
		}
	}

	private static class CommonSetupModule extends BinderModule {

		@Override
		protected void declare() {
			// some configuration value definitions
			configure(Bean.class).bind("a", String.class).to("a");
			configure(Bean.class).bind("b", Integer.class).to(42);

			// assume somewhere else (in another module) we declare
			construct(Bean.class);

			// and also we need a converter Integer => String
			bind(Converter.converterTypeOf(Integer.class, String.class)).to(String::valueOf);
		}
	}

	static UnresolvableDependency.SupplyFailed supplyFailedSupplier() {
		return new UnresolvableDependency.SupplyFailed(
				"No such configuration value or converter",
				new NoSuchElementException());
	}

	@Installs(bundles = CommonSetupModule.class)
	private static class Solution1 extends BinderModule {

		@Override
		protected void declare() {
			// in some general setup module we declare the mechanism of
			// providing config values
			bindConfigPropertySupplier(Type.WILDCARD);
		}

		private <T> void bindConfigPropertySupplier(Type<T> type) {
			per(Scope.dependencyInstance) //
					.bind(Name.ANY.in(named(ConfigProperty.class)), type) //
					.toSupplier((dep, context) ->
							resolveConfigProperty(dep, context, dep.at().element()));
		}

		@SuppressWarnings("unchecked")
		private static <T> T resolveConfigProperty(Dependency<? super T> dep,
				Injector context, AnnotatedElement e) {
			ConfigProperty property = e.getAnnotation(ConfigProperty.class);
			Config config = context.resolve(dep.uninject().onInstance(
					Instance.defaultInstanceOf(raw(Config.class))));
			return (T) config.value(property.from(), property.value())
					.as(dep.type()).orElseThrow(
							TestExampleConfigPropertyAnnotationBinds::supplyFailedSupplier);
		}
	}

	/**
	 * Part of the solution is that we set a {@link HintsBy} strategy that
	 * creates the appropriate {@link Hint} that then is resolved to the special
	 * {@link Supplier} we have bound in {@link Solution1}.
	 */
	private static Injector bootstrapSolution1() {
		return Bootstrap.injector(Bootstrap.DEFAULT_ENV.with(HintsBy.class,
				HintsBy.instanceReference(
						NamesBy.annotatedWith(ConfigProperty.class,
								ConfigProperty::value, true))),
				Solution1.class);
	}

	/**
	 * An alternative to {@link Solution1} that does not use a {@link Supplier}.
	 * Instead the {@link Hint} computed by the custom {@link HintsBy} strategy
	 * directly resolves the {@link ConfigProperty} and provides it as a {@link
	 * Hint#constant(Object)}.
	 * <p>
	 * In contrast to {@link Solution1} this solution cannot consider the full
	 * {@link Dependency} when resolving the {@link Config}. It is limited to
	 * the {@link Class} the {@link java.lang.reflect.Parameter} belongs to.
	 * Therefore more sophisticated targeting would not work as expected.
	 */
	private static Injector bootstrapSolution2() {
		return Bootstrap.injector(Bootstrap.DEFAULT_ENV.with(HintsBy.class,
				((param, context) -> {
					if (!param.isAnnotationPresent(ConfigProperty.class))
						return null;
					Config config = context.resolve(dependency(raw(Config.class))
							.injectingInto(raw(param.getDeclaringExecutable().getDeclaringClass())));
					ConfigProperty property = param.getAnnotation(ConfigProperty.class);
					Type<?> parameterType = parameterType(param);
					return Hint.constant(config.value(property.from(), property.value())
							.as(parameterType)
							.orElseThrow(TestExampleConfigPropertyAnnotationBinds::supplyFailedSupplier))
							.asType(parameterType);
				})), CommonSetupModule.class);
	}

	@Test
	void configPropertyIsInjectedAsAskedByTheAnnotation1() {
		assertEquals("a", bootstrapSolution1().resolve(Bean.class).a);
	}

	@Test
	void configPropertyIsInjectedAndConvertedAsAskedByTheAnnotation1() {
		assertEquals("42", bootstrapSolution1().resolve(Bean.class).b);
	}

	@Test
	void configPropertyIsInjectedAsAskedByTheAnnotation2() {
		assertEquals("a", bootstrapSolution2().resolve(Bean.class).a);
	}

	@Test
	void configPropertyIsInjectedAndConvertedAsAskedByTheAnnotation2() {
		assertEquals("42", bootstrapSolution2().resolve(Bean.class).b);
	}
}
