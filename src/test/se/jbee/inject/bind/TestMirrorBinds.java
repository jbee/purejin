package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static se.jbee.inject.container.Cast.injectionCaseTypeFor;

import org.junit.Test;

import se.jbee.inject.InjectionCase;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.ScopingMirror;
import se.jbee.inject.util.Scoped;

public class TestMirrorBinds {

	private static class MirrorBindsModule extends BinderModule {

		@Override
		protected Bindings configure(Bindings bindings) {
			return bindings.with(bindings.mirrors.scopeBy(
					ScopingMirror.alwaysDefault.unlessAnnotatedWith(
							Scoped.class)));
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
		InjectionCase<InjectionScoped> icase = injector.resolve(
				injectionCaseTypeFor(InjectionScoped.class));
		assertEquals(Scope.injection, icase.scoping.scope);
	}
}
