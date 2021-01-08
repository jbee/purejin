package se.jbee.inject.binder;

import se.jbee.inject.Env;
import se.jbee.inject.Scope;
import se.jbee.inject.bind.Module;
import se.jbee.inject.bind.*;

import static se.jbee.inject.Source.source;

/**
 * An alternative simplified binder API that is particularly useful as it does
 * not require any {@link Env} properties to work.
 * <p>
 * Therefore it is for example used to bootstrap the initial {@link Env}.
 * <p>
 * By default this API binds all bindings as {@link se.jbee.inject.DeclarationType#DEFAULT}.
 *
 * @since 8.1
 */
public abstract class SimpleModule extends SimpleBinder
		implements Bundle, Module {

	private Boolean declaring;

	protected SimpleModule() {
		super(Bind.UNINITIALIZED.per(Scope.container));
	}

	protected abstract void declare();

	@Override
	public final void bootstrap(Bootstrapper bootstrap) {
		bootstrap.install(this);
	}

	@Override
	public final void declare(Bindings bindings, Env env) {
		InconsistentBinding.nonnullThrowsReentranceException(declaring);
		configure(bind -> bind
				.into(env, bindings)
				.with(source(getClass()))
				.asDefault());
		declaring = true;
		declare();
	}

}
