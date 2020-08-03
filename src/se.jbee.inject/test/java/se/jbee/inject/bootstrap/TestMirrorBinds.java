package se.jbee.inject.bootstrap;

import org.junit.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.config.ScopesBy;
import se.jbee.inject.util.Scoped;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static se.jbee.inject.Cast.resourceTypeFor;

public class TestMirrorBinds {

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
	private static final class InjectionScoped {

	}

	private final Injector injector = Bootstrap.injector(
			MirrorBindsModule.class);

	@Test
	public void annotatedScopeIsUsed() {
		assertNotSame(injector.resolve(InjectionScoped.class),
				injector.resolve(InjectionScoped.class));
		Resource<InjectionScoped> resource = injector.resolve(
				resourceTypeFor(InjectionScoped.class));
		assertEquals(Scope.injection, resource.permanence.scope);
	}
}
