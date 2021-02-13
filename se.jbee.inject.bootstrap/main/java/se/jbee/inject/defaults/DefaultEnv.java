package se.jbee.inject.defaults;

import se.jbee.inject.*;
import se.jbee.inject.bind.Binding;
import se.jbee.inject.bind.BindingConsolidation;
import se.jbee.inject.bind.Bindings;
import se.jbee.inject.binder.*;
import se.jbee.inject.config.*;
import se.jbee.inject.container.Container;
import se.jbee.lang.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static se.jbee.lang.Type.raw;

/**
 * The {@link se.jbee.inject.bind.Module} that setups the values required in an
 * {@link Env} used with the {@link Binder} API.
 */
public class DefaultEnv extends SimpleModule {

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
		bindValueBinder(Binding.class).to(DefaultValueBinders.PUBLISHED_APIS);
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
		bind(PublishesBy.class).to(PublishesBy.PROTECTIVE);

		// reflection
		bind(Get.class).to(Field::get);
		bind(Invoke.class).to(Method::invoke);
		bind(New.class).to(Constructor::newInstance);
		inPackageAndSubPackagesOf(DefaultEnv.class).bind(New.class).to(DefaultEnv::newInstance);
		in(Packages.TEST).bind(New.class).to(DefaultEnv::newInstance);

		// how to cull and verify the Bindings made
		bind(BindingConsolidation.class).to(DefaultBindingConsolidation::consolidate);

		// verification is off
		bind(Env.USE_VERIFICATION, boolean.class).to(false);

		// extras
		bind(Plugins.class).toFactory(Plugins::new);
		bind(Annotated.Enhancer.class).to(Annotated.SOURCE);
	}

	private static <T> T newInstance(Constructor<T> target, Object[] args) throws Exception {
		target.setAccessible(true);
		return target.newInstance(args);
	}
}
