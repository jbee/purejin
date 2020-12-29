package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.ScopesBy;
import test.integration.util.Scoped;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * This test show how to override the default {@link ScopesBy} in the {@link
 * Env} to allow explicitly setting the {@link Scope} of bound instances using
 * {@link java.lang.annotation.Annotation}s.
 * <p>
 * This build upon that the default {@link Scope} is {@link Scope#auto} which is
 * a "virtual" scope indicating that {@link ScopesBy} should be used to
 * determine the used {@link Scope}. Should a binding explicitly set a {@link
 * Scope} using {@link se.jbee.inject.binder.Binder.RootBinder#per(Name)} this
 * still is honoured and the custom {@link ScopesBy} strategy is not used for
 * such a binding.
 */
class TestExampleAnnotatedScopeBinds {

	private static class TestExampleAnnotatedScopeBindsModule extends BinderModule {

		@Override
		public Env configure(Env env) {
			return env.with(ScopesBy.class, //
							ScopesBy.annotatedWith(Scoped.class, Scoped::value));
		}

		@Override
		protected void declare() {
			bind(InjectionScoped.class).toConstructor();
		}
	}

	@Scoped("injection")
	public static final class InjectionScoped {

	}

	private final Injector injector = Bootstrap.injector(
			TestExampleAnnotatedScopeBindsModule.class);

	@Test
	void annotatedScopeIsUsed() {
		assertNotSame(injector.resolve(InjectionScoped.class),
				injector.resolve(InjectionScoped.class));
		Resource<InjectionScoped> resource = injector.resolve(
				Resource.resourceTypeOf(InjectionScoped.class));
		assertEquals(Scope.injection, resource.lifeCycle.scope);
	}
}
