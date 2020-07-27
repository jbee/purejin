/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.instance;
import static se.jbee.inject.Utils.isClassBanal;
import static se.jbee.inject.Utils.isClassInstantiable;
import static se.jbee.inject.bootstrap.Supply.byAccess;
import static se.jbee.inject.bootstrap.Supply.byNew;
import static se.jbee.inject.bootstrap.Supply.byParametrizedInstanceReference;
import static se.jbee.inject.bootstrap.Supply.byProducer;
import static se.jbee.inject.declare.BindingType.CONSTRUCTOR;
import static se.jbee.inject.declare.BindingType.FIELD;
import static se.jbee.inject.declare.BindingType.METHOD;
import static se.jbee.inject.declare.BindingType.PREDEFINED;
import static se.jbee.inject.declare.BindingType.REFERENCE;
import static se.jbee.inject.declare.Bindings.supplyConstant;
import static se.jbee.inject.declare.Bindings.supplyScopedConstant;

import java.lang.reflect.Constructor;

import se.jbee.inject.DeclarationType;
import se.jbee.inject.Env;
import se.jbee.inject.Instance;
import se.jbee.inject.Locator;
import se.jbee.inject.Parameter;
import se.jbee.inject.Type;
import se.jbee.inject.config.ConstructsBy;
import se.jbee.inject.container.Supplier;
import se.jbee.inject.declare.Binding;
import se.jbee.inject.declare.BindingType;
import se.jbee.inject.declare.Bindings;
import se.jbee.inject.declare.InconsistentBinding;
import se.jbee.inject.declare.ValueBinder;

/**
 * Utility with default {@link ValueBinder}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class DefaultBinders {

	public static final ValueBinder<Binding<?>> SUPER_TYPES = new SuperTypesBinder();
	public static final ValueBinder<Class<?>> PARAMETRIZED_REF = new TypeParametrizedReferenceBinder();
	public static final ValueBinder<Instance<?>> INSTANCE_REF = new ReferenceBinder(
			false);
	public static final ValueBinder<Instance<?>> INSTANCE_REF_LITE = new ReferenceBinder(
			true);
	public static final ValueBinder<Parameter<?>[]> ARRAY = new ArrayElementsBinder();
	public static final ValueBinder<New<?>> NEW = new NewBinder();
	public static final ValueBinder<Produces<?>> PRODUCES = new ProducesBinder();
	public static final ValueBinder<Shares<?>> SHARES = new SharesBinder();
	public static final ValueBinder<Constant<?>> CONSTANT = new ConstantBinder();

	/**
	 * This {@link ValueBinder} adds bindings to super-types for
	 * {@link Binding}s declared with {@link DeclarationType#AUTO} or
	 * {@link DeclarationType#PROVIDED}.
	 */
	static final class SuperTypesBinder implements ValueBinder<Binding<?>> {

		@Override
		public <T> void expand(Env env, Binding<?> src, Binding<T> item,
				Bindings target) {
			target.add(item);
			DeclarationType declarationType = item.source.declarationType;
			if (declarationType != DeclarationType.AUTO
				&& declarationType != DeclarationType.PROVIDED)
				return;
			for (Type<? super T> supertype : item.type().supertypes())
				// Object is of course a superclass but not indented when doing auto-binds
				if (supertype.rawType != Object.class)
					target.add(item.typed(supertype));
		}
	}

	static final class ArrayElementsBinder
			implements ValueBinder.Completion<Parameter<?>[]> {

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <T> Binding<T> complete(Binding<T> item,
				Parameter<?>[] elements) {
			return item.complete(PREDEFINED,
					supplier((Type) item.type(), elements));
		}

		@SuppressWarnings("unchecked")
		static <E> Supplier<E> supplier(Type<E[]> array,
				Parameter<?>[] elements) {
			return (Supplier<E>) Supply.fromElements(array,
					(Parameter<? extends E>[]) elements);
		}

	}

	static final class TypeParametrizedReferenceBinder
			implements ValueBinder.Completion<Class<?>> {

		@Override
		public <T> Binding<T> complete(Binding<T> item, Class<?> to) {
			return item.complete(REFERENCE, byParametrizedInstanceReference(
					anyOf(raw(to).castTo(item.type()))));
		}

	}

	static final class NewBinder implements ValueBinder.Completion<New<?>> {

		@Override
		public <T> Binding<T> complete(Binding<T> item, New<?> src) {
			return item.complete(CONSTRUCTOR, byNew(src.typed(item.type())));
		}
	}

	static final class ProducesBinder
			implements ValueBinder.Completion<Produces<?>> {

		@Override
		public <T> Binding<T> complete(Binding<T> item, Produces<?> src) {
			return item.complete(METHOD, byProducer(src.typed(item.type())));
		}
	}

	static final class SharesBinder
			implements ValueBinder.Completion<Shares<?>> {

		@Override
		public <T> Binding<T> complete(Binding<T> item, Shares<?> src) {
			return item.complete(FIELD, byAccess(src.typed(item.type())));
		}

	}

	static final class ConstantBinder implements ValueBinder<Constant<?>> {

		@Override
		public <T> void expand(Env env, Constant<?> src, Binding<T> item,
				Bindings target) {
			T constant = item.type().rawType.cast(src.value);
			Supplier<T> supplier = src.scoped
				? supplyScopedConstant(constant)
				: supplyConstant(constant);
			target.addExpanded(env,
					item.complete(BindingType.PREDEFINED, supplier));
			Class<?> impl = src.value.getClass();
			// implicitly bind to the exact type of the constant
			// should that differ from the binding type
			if (src.autoBindExactType
				&& item.source.declarationType == DeclarationType.EXPLICIT
				&& item.type().rawType != impl) {
				@SuppressWarnings("unchecked")
				Class<T> type = (Class<T>) src.value.getClass();
				target.addExpanded(env,
						Binding.binding(item.locator.typed(raw(type)),
								BindingType.PREDEFINED, supplier, item.scope,
								item.source.typed(DeclarationType.IMPLICIT)));
			}
		}
	}

	static final class ReferenceBinder implements ValueBinder<Instance<?>> {

		private final boolean avoidReferences;

		ReferenceBinder(boolean avoidReferences) {
			this.avoidReferences = avoidReferences;
		}

		@Override
		public <T> void expand(Env env, Instance<?> src, Binding<T> item,
				Bindings target) {
			Type<?> srcType = src.type();
			if (avoidReferences && isClassBanal(srcType.rawType)) {
				target.addExpanded(env, item,
						new Constant<>(instance(srcType.rawType)).manual());
				return;
			}
			if (srcType.isAssignableTo(raw(Supplier.class))
				&& !item.type().isAssignableTo(raw(Supplier.class))) {
				@SuppressWarnings("unchecked")
				Class<? extends Supplier<? extends T>> supplier = (Class<? extends Supplier<? extends T>>) srcType.rawType;
				target.addExpanded(env, item.complete(REFERENCE,
						Supply.bySupplierReference(supplier)));
				implicitlyBindToConstructor(env, src, item, target);
				return;
			}
			final Type<? extends T> type = srcType.castTo(item.type());
			final Instance<T> bound = item.locator.instance;
			if (!bound.type().equalTo(type)
				|| !src.name.isCompatibleWith(bound.name)) {
				target.addExpanded(env, item.complete(REFERENCE,
						Supply.byInstanceReference(src.typed(type))));
				implicitlyBindToConstructor(env, src, item, target);
				return;
			}
			if (type.isInterface())
				throw InconsistentBinding.loop(item, src, bound);
			bindToMirrorConstructor(env, type.rawType, item, target);
		}

	}

	static <T> void implicitlyBindToConstructor(Env env, Instance<T> src,
			Binding<?> item, Bindings target) {
		Class<T> impl = src.type().rawType;
		if (isClassInstantiable(impl)) {
			Binding<T> binding = Binding.binding(
					new Locator<>(src).indirect(item.locator.target.indirect),
					BindingType.CONSTRUCTOR, null, item.scope,
					item.source.typed(DeclarationType.IMPLICIT));
			bindToMirrorConstructor(env, impl, binding, target);
		}
	}

	static <T> void bindToMirrorConstructor(Env env, Class<? extends T> src,
			Binding<T> item, Bindings target) {
		Constructor<? extends T> c = env.property(ConstructsBy.class,
				item.source.pkg()).reflect(src);
		if (c != null)
			target.addExpanded(env, item, New.bind(c));
	}

}
