package se.jbee.inject.binder;

import se.jbee.inject.*;
import se.jbee.inject.bind.Bind;
import se.jbee.inject.bind.Bindings;
import se.jbee.inject.bind.ValueBinder;
import se.jbee.inject.binder.spi.*;
import se.jbee.lang.Type;

import java.util.function.UnaryOperator;

import static se.jbee.inject.bind.BindingType.PREDEFINED;

/**
 * A simple fluent binder API that only allows to bind to constants or provided
 * {@link Supplier}s, {@link Generator}s or factory methods.
 *
 * The main role of the simple API is to be used during the bootstrapping of
 * the default {@link Env} where no property from the {@link Env} is available.
 * Therefore this API does not use any of the abstractions between the fluent
 * API and the {@link Bindings} (like {@link ValueBinder}s) since these would
 * require an already defined {@link Env}.
 *
 * @since 8.1
 */
public class SimpleBinder implements
		InstanceLocalBinder<SimpleBinder>,
		ParentLocalBinder<SimpleBinder>,
		PackageLocalBinder<SimpleBinder> {

	private Bind bind;

	protected SimpleBinder(Bind bind) {
		this.bind = bind;
	}

	protected final void configure(UnaryOperator<Bind> f) {
		this.bind = f.apply(bind);
	}

	@Override
	public final SimpleBinder in(Packages packages) {
		return new SimpleBinder(bind.with(bind.target.in(packages)));
	}

	@Override
	public final SimpleBinder within(Instance<?> parent) {
		return new SimpleBinder(bind.with(bind.target.within(parent)));
	}

	@Override
	public final SimpleBinder injectingInto(Instance<?> target) {
		return new SimpleBinder(bind.with(Target.targeting(target)));
	}

	public final <T> TypedSimpleBinder<T> bind(Class<T> type) {
		return bind(Type.raw(type));
	}

	public final <T> TypedSimpleBinder<T> bind(Type<T> type) {
		return bind(Name.DEFAULT, type);
	}

	public final <T> TypedSimpleBinder<T> bind(String name, Class<T> type) {
		return bind(Name.named(name), Type.raw(type));
	}

	public <T> TypedSimpleBinder<T> bind(Name name, Type<T> type) {
		return new TypedSimpleBinder<>(Instance.instance(name, type), bind);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final <T extends Descriptor> TypedSimpleBinder<ValueBinder<? extends T>> bindValueBinder(
			Class<T> property) {
		return bind((Type) ValueBinder.valueBinderTypeOf(property));
	}

	public static final class TypedSimpleBinder<T> implements SupplierBinder<T>,
			ReferenceBinder<T> {

		private final Instance<T> bound;
		private final Bind bind;

		protected TypedSimpleBinder(Instance<T> bound, Bind bind) {
			this.bound = bound;
			this.bind = bind.next();
		}

		public void to(T constant) {
			addBindingWith(Bindings.supplyConstant(constant));
		}

		@Override
		public void toSupplier(Supplier<? extends T> supplier) {
			addBindingWith(supplier);
		}

		@Override
		public <I extends T> void to(Instance<I> instance) {
			addBindingWith(Supply.byInstanceReference(instance));
		}

		private void addBindingWith(Supplier<? extends T> supplier) {
			bind.bindings.add(bind.env,
					bind.asType(new Locator<>(bound, bind.target), PREDEFINED,
							supplier));
		}
	}
}
