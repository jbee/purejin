/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.defaults;

import se.jbee.inject.*;
import se.jbee.inject.bind.*;
import se.jbee.inject.binder.*;
import se.jbee.inject.config.ConstructsBy;
import se.jbee.inject.lang.Reflect;
import se.jbee.inject.lang.Type;

import java.lang.reflect.Constructor;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.bind.BindingType.*;
import static se.jbee.inject.bind.Bindings.supplyConstant;
import static se.jbee.inject.bind.Bindings.supplyScopedConstant;
import static se.jbee.inject.binder.Supply.*;
import static se.jbee.inject.lang.Type.raw;
import static se.jbee.inject.lang.Utils.isClassBanal;
import static se.jbee.inject.lang.Utils.isClassConstructable;

/**
 * Utility with default {@link ValueBinder}s.
 */
public final class DefaultValueBinders {

	public static final ValueBinder.Completion<Ref.BridgeRef> BRIDGE = DefaultValueBinders::bindGenericReference;
	public static final ValueBinder.Completion<Ref.ArrayRef> ARRAY = DefaultValueBinders::bindArrayElements;
	public static final ValueBinder.Completion<Constructs<?>> CONSTRUCTS = DefaultValueBinders::bindConstruction;
	public static final ValueBinder.Completion<Produces<?>> PRODUCES = DefaultValueBinders::bindProduction;
	public static final ValueBinder.Completion<Accesses<?>> SHARES = DefaultValueBinders::bindAccess;
	public static final ValueBinder<Instance<?>> REFERENCE = DefaultValueBinders::bindReference;
	public static final ValueBinder<Instance<?>> REFERENCE_PREFER_CONSTANTS = DefaultValueBinders::bindReferencePreferConstants;
	public static final ValueBinder<Constant<?>> CONSTANT = DefaultValueBinders::bindConstant;
	public static final ValueBinder<Binding<?>> SUPER_TYPES = DefaultValueBinders::bindSuperTypes;

	private DefaultValueBinders() {
		throw new UnsupportedOperationException("util");
	}

	/**
	 * This {@link ValueBinder} adds bindings to super-types for {@link
	 * Binding}s declared with {@link DeclarationType#SUPER} or {@link
	 * DeclarationType#PROVIDED}.
	 */
	private static <T> void bindSuperTypes(Env env, Binding<?> ref,
			Binding<T> item, Bindings target) {
		target.add(env, item);
		DeclarationType declarationType = item.source.declarationType;
		if (declarationType != DeclarationType.SUPER && declarationType != DeclarationType.PROVIDED)
			return;
		for (Type<? super T> supertype : item.type().supertypes())
			// Object is of course a superclass but not indented when doing super-binds
			if (supertype.rawType != Object.class)
				target.add(env, item.typed(supertype));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T> Binding<T> bindArrayElements(Env env, Binding<T> item,
			Ref.ArrayRef ref) {
		return item.complete(PREDEFINED,
				createArraySupplier((Type) item.type(), ref.elements));
	}

	@SuppressWarnings("unchecked")
	private static <E> Supplier<E> createArraySupplier(Type<E[]> array,
			Hint<?>[] elements) {
		return (Supplier<E>) Supply.fromElements(array,
				(Hint<? extends E>[]) elements);
	}

	private static <T> Binding<T> bindGenericReference(Env env, Binding<T> item,
			Ref.BridgeRef ref) {
		return item.complete(BindingType.REFERENCE,
				byParameterizedInstanceReference(
						anyOf(raw(ref.type).castTo(item.type()))));
	}

	private static <T> Binding<T> bindConstruction(Env env, Binding<T> item,
			Constructs<?> ref) {
		env.accessible(ref.target);
		return item.complete(CONSTRUCTOR, byConstruction(ref.typed(item.type()))) //
				.verifiedBy(env.verifierFor(ref));
	}

	private static <T> Binding<T> bindProduction(Env env, Binding<T> item,
			Produces<?> ref) {
		env.accessible(ref.target);
		return item.complete(METHOD, byProduction(ref.typed(item.type()))) //
				.verifiedBy(env.verifierFor(ref));
	}

	private static <T> Binding<T> bindAccess(Env env, Binding<T> item,
			Accesses<?> ref) {
		env.accessible(ref.target);
		// reference itself is used as supplier, as it also provides the default implementation
		return item.complete(FIELD, ref.typed(item.type())) //
				.verifiedBy(env.verifierFor(ref));
	}

	private static <T> void bindConstant(Env env, Constant<?> ref,
			Binding<T> item, Bindings target) {
		T constant = item.type().rawType.cast(ref.value);
		Supplier<T> supplier = ref.scoped
				? supplyScopedConstant(constant)
				: supplyConstant(constant);
		target.addExpanded(env,
				item.complete(BindingType.PREDEFINED, supplier));
		Class<?> impl = ref.value.getClass();
		// implicitly bind to the exact type of the constant
		// should that differ from the binding type
		if (ref.autoBindExactType && item.source.declarationType == DeclarationType.EXPLICIT && item.type().rawType != impl) {
			@SuppressWarnings("unchecked")
			Class<T> type = (Class<T>) ref.value.getClass();
			target.addExpanded(env,
					Binding.binding(item.signature.typed(raw(type)),
							BindingType.PREDEFINED, supplier, item.scope,
							item.source.typed(DeclarationType.IMPLICIT)));
		}
	}

	private static <T> void bindReferencePreferConstants(Env env,
			Instance<?> ref, Binding<T> item, Bindings target) {
		Type<?> refType = ref.type();
		if (isClassBanal(refType.rawType)) {
			target.addExpanded(env, item, new Constant<>(
					Reflect.construct(refType.rawType, env::accessible,
							RuntimeException::new)).manual());
			//TODO shouldn't this use New instead?
			return;
		}
		bindReference(env, ref, item, target);
	}

	private static <T> void bindReference(Env env, Instance<?> ref,
			Binding<T> item, Bindings target) {
		Type<?> refType = ref.type();
		if (isCompatibleSupplier(item.type(), refType)) {
			@SuppressWarnings("unchecked")
			Class<? extends Supplier<? extends T>> supplier = (Class<? extends Supplier<? extends T>>) refType.rawType;
			target.addExpanded(env, item.complete(BindingType.REFERENCE,
					Supply.bySupplierReference(supplier)));
			implicitlyBindToConstructor(env, ref, item, target);
			return;
		}
		final Type<? extends T> type = refType.castTo(item.type());
		final Instance<T> bound = item.signature.instance;
		if (!bound.type().equalTo(type) || !ref.name.isCompatibleWith(
				bound.name)) {
			target.addExpanded(env, item.complete(BindingType.REFERENCE,
					Supply.byInstanceReference(ref.typed(type))));
			implicitlyBindToConstructor(env, ref, item, target);
			return;
		}
		if (type.isInterface())
			throw InconsistentBinding.referenceLoop(item, ref, bound);
		bindToConstructsBy(env, type.rawType, item, target);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T> boolean isCompatibleSupplier(Type<T> requiredType,
			Type<?> providedType) {
		if (!providedType.isAssignableTo(raw(Supplier.class)))
			return false;
		if (requiredType.isAssignableTo(raw(Supplier.class)))
			return false;
		return Type.supertype(Supplier.class, (Type) providedType) //
				.parameter(0).isAssignableTo(requiredType);
	}

	private static <T> void implicitlyBindToConstructor(Env env,
			Instance<T> ref, Binding<?> item, Bindings target) {
		Class<T> impl = ref.type().rawType;
		if (isClassConstructable(impl)) {
			Binding<T> binding = Binding.binding(
					new Locator<>(ref).indirect(item.signature.target.indirect),
					BindingType.CONSTRUCTOR, null, item.scope,
					item.source.typed(DeclarationType.IMPLICIT));
			bindToConstructsBy(env, impl, binding, target);
		}
	}

	private static <T> void bindToConstructsBy(Env env, Class<? extends T> ref,
			Binding<T> item, Bindings target) {
		Constructor<?> c = env.property(ConstructsBy.class,
				item.source.pkg()).reflect(ref.getDeclaredConstructors());
		if (c != null)
			target.addExpanded(env, item,
					Constructs.constructs(raw(c.getDeclaringClass()), c));
	}
}
