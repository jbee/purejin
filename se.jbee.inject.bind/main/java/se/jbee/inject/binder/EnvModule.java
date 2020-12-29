package se.jbee.inject.binder;

import se.jbee.inject.Env;
import se.jbee.inject.Extends;
import se.jbee.inject.Name;
import se.jbee.inject.Scope;
import se.jbee.inject.bind.Bind;
import se.jbee.inject.config.ContractsBy;

/**
 * @since 8.1
 */
@Extends(Env.class)
public abstract class EnvModule extends BinderModule {

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

	@Override
	protected final boolean installDefaults() {
		return false;
	}
}
