package se.jbee.inject.binder;

import se.jbee.inject.*;
import se.jbee.inject.bind.Module;
import se.jbee.inject.bind.*;
import se.jbee.inject.lang.Type;

import java.util.function.Function;

import static se.jbee.inject.Source.source;
import static se.jbee.inject.bind.BindingType.PREDEFINED;

/**
 * A alternative simplified binder API that is particularly useful as it does
 * not require any {@link Env} properties to work.
 *
 * Therefore it is for example used to bootstrap the initial {@link Env}.
 *
 * @since 8.1
 */
public abstract class ConstantsModule implements Bundle, Module {

	private Bind bind;

	protected abstract void declare();

	@Override
	public final void bootstrap(Bootstrapper bootstrap) {
		bootstrap.install(this);
	}

	@Override
	public void declare(Bindings bindings, Env env) {
		InconsistentBinding.nonnullThrowsReentranceException(bind);
		this.bind = Bind.UNINITIALIZED //
				.into(env, bindings) //
				.with(source(getClass()))
				.asDefault() //
				.per(Scope.container);
		declare();
	}

	public final <T> ConstantBinder<T> bind(Class<T> type) {
		return bind(Type.raw(type));
	}

	public final <T> ConstantBinder<T> bind(Type<T> type) {
		return bind(Name.DEFAULT, type);
	}

	public final <T> ConstantBinder<T> bind(String name, Class<T> type) {
		return bind(Name.named(name), Type.raw(type));
	}

	public <T> ConstantBinder<T> bind(Name name, Type<T> type) {
		return new ConstantBinder<>(Instance.instance(name, type), bind);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public final <T extends Descriptor> ConstantBinder<ValueBinder<? extends T>> bindValueBinder(Class<T> property) {
		return bind((Type) ValueBinder.valueBinderTypeOf(property));
	}

	public static class ConstantBinder<T> {

		private final Instance<T> bound;
		private final Bind bind;

		protected ConstantBinder(Instance<T> bound, Bind bind) {
			this.bound = bound;
			this.bind = bind.next();
		}

		public void to(T constant) {
			addWith(Bindings.supplyConstant(constant));
		}

		public void toSupplier(Supplier<? extends T> supplier) {
			addWith(supplier);
		}

		public void toFactory(Function<Injector, T> factory) {
			toSupplier(((dep, context) -> factory.apply(context)));
		}

		public void toGenerator(Generator<? extends T> generator) {
			addWith(Supplier.nonScopedBy(generator));
		}

		private void addWith(Supplier<? extends T> supplier) {
			bind.bindings.add(bind.env,
					bind.asType(new Locator<>(bound), PREDEFINED, supplier));
		}
	}
}
