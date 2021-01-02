package se.jbee.inject.binder;

import se.jbee.inject.Env;
import se.jbee.inject.Extends;
import se.jbee.inject.Name;
import se.jbee.inject.Scope;
import se.jbee.inject.bind.Bind;
import se.jbee.inject.config.ContractsBy;

/**
 * Base {@link se.jbee.inject.bind.Module} for usage when declaring an {@link
 * Env}.
 * <p>
 * The important difference are:
 * <p>
 * {@link #installDefaults()} is {@code false}, meaning no "application context"
 * basics are installed. These are not used in an {@link Env}.
 * <p>
 * This class is annotated with {@link Extends} referring to {@link Env} which
 * means should the extending class be stated as provider of a {@link
 * se.jbee.inject.bind.Bundle} for the {@link java.util.ServiceLoader} it is
 * understood as belonging to the {@link Env}. This is a convention then picked
 * up by {@link ServiceLoaderEnvBundles}.
 *
 * @since 8.1
 */
@Extends(Env.class)
public abstract class EnvModule extends BinderModule {

	@Override
	protected final boolean installDefaults() {
		return false;
	}

	@Override
	protected Bind init(Bind bind) {
		return bind.per(Scope.container);
	}

	public final void addContract(Class<?> api) {
		injectingInto(Env.class).plug(api).into(ContractsBy.class);
	}

	public final TypedBinder<ContractsBy> bindContractsBy() {
		return bind(ContractsBy.class);
	}

	public final TypedBinder<ContractsBy> bindContractsByOf(Class<?> type) {
		return bind(Name.named(type), ContractsBy.class);
	}

}
