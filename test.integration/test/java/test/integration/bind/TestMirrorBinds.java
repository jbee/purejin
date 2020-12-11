package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Environment;
import se.jbee.inject.config.ScopesBy;
import test.integration.util.Scoped;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static se.jbee.inject.Resource.resourceTypeOf;

class TestMirrorBinds {

	private static class MirrorBindsModule extends BinderModule {

		@Override
		protected Env configure(Env env) {
			return Environment.override(env) //
					.with(ScopesBy.class, ScopesBy.alwaysDefault //
							.unlessAnnotatedWith(Scoped.class));
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
			MirrorBindsModule.class);

	@Test
	void annotatedScopeIsUsed() {
		assertNotSame(injector.resolve(InjectionScoped.class),
				injector.resolve(InjectionScoped.class));
		Resource<InjectionScoped> resource = injector.resolve(
				Resource.resourceTypeOf(InjectionScoped.class));
		assertEquals(Scope.injection, resource.permanence.scope);
	}
}
