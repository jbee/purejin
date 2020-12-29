package se.jbee.inject.defaults;

import se.jbee.inject.*;
import se.jbee.inject.bind.Binding;
import se.jbee.inject.bind.BindingConsolidation;
import se.jbee.inject.bind.Bindings;
import se.jbee.inject.binder.*;
import se.jbee.inject.config.*;
import se.jbee.inject.container.Container;
import se.jbee.inject.lang.Type;

import static se.jbee.inject.lang.Type.raw;

public class DefaultEnv extends ConstantsModule {

	public static Env bootstrap() {
		return Container.injector(Bindings.newBindings()
				.declaredFrom(DefaultEnv::property, new DefaultEnv())).asEnv();
	}

	/**
	 * This method implements the {@link Env} used to bootstrap the {@link DefaultEnv}.
	 *
	 * It simply does not know anything and throws an {@link InconsistentDeclaration}.
	 */
	private static <T> T property(Name qualifier, Type<T> property, Class<?> ns) {
		throw new InconsistentDeclaration(String.format(
				"Bootstrapping Env has no properties but tried to resolve: %s %s in %s",
				qualifier, property.toString(), ns));
	}

	@Override
	protected void declare() {
		// minimum preamble
		bind(Scope.container, raw(ScopeLifeCycle.class)).to(ScopeLifeCycle.container);

		// ValueBinders
		bindValueBinder(Binding.class).to(DefaultValueBinders.CONTRACTS);
		bindValueBinder(Constructs.class).to(DefaultValueBinders.CONSTRUCTS);
		bindValueBinder(Constant.class).to(DefaultValueBinders.CONSTANT);
		bindValueBinder(Produces.class).to(DefaultValueBinders.PRODUCES);
		bindValueBinder(Accesses.class).to(DefaultValueBinders.ACCESSES);
		bindValueBinder(Instance.class).to(DefaultValueBinders.REFERENCE);
		bindValueBinder(Descriptor.BridgeDescriptor.class).to(DefaultValueBinders.BRIDGE);
		bindValueBinder(Descriptor.ArrayDescriptor.class).to(DefaultValueBinders.ARRAY);

		// strategies
		bind(ConstructsBy.class).to(ConstructsBy.OPTIMISTIC);
		bind(AccessesBy.class).to(impl -> null);
		bind(ProducesBy.class).to(impl -> null);
		bind(NamesBy.class).to(obj -> Name.DEFAULT);
		bind(ScopesBy.class).to(ScopesBy.AUTO);
		bind(HintsBy.class).to(((param, context) -> null));
		bind(ContractsBy.class).to(ContractsBy.PROTECTIVE);

		// how to cull and verify the Bindings made
		bind(BindingConsolidation.class).to(DefaultBindingConsolidation::consolidate);

		// do not use deep reflection (set accessible) (but if enabled use it everywhere)
		bind(Env.USE_DEEP_REFLECTION, boolean.class).to(false);
		bind(Env.DEEP_REFLECTION_PACKAGES, Packages.class).to(Packages.ALL);

		// verification is off
		bind(Env.USE_VERIFICATION, boolean.class).to(false);

		// extras
		bind(Plugins.class).toFactory(Plugins::new);
		bind(Annotated.Enhancer.class).to(Annotated.SOURCE);
	}
}
