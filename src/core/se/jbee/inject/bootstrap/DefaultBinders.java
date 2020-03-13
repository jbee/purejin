/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.DeclarationType.MULTI;
import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.isClassInstantiable;
import static se.jbee.inject.bootstrap.Supply.constructor;
import static se.jbee.inject.bootstrap.Supply.method;
import static se.jbee.inject.bootstrap.Supply.parametrizedInstance;
import static se.jbee.inject.config.Plugins.pluginPoint;
import static se.jbee.inject.declare.BindingType.CONSTRUCTOR;
import static se.jbee.inject.declare.BindingType.METHOD;
import static se.jbee.inject.declare.BindingType.PREDEFINED;
import static se.jbee.inject.declare.BindingType.REFERENCE;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import se.jbee.inject.DeclarationType;
import se.jbee.inject.Env;
import se.jbee.inject.Instance;
import se.jbee.inject.Locator;
import se.jbee.inject.Name;
import se.jbee.inject.Parameter;
import se.jbee.inject.Source;
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
	public static final ValueBinder<Instance<?>> INSTANCE_REF = new ReferenceBinder();
	public static final ValueBinder<Parameter<?>[]> ARRAY = new ArrayElementsBinder();
	public static final ValueBinder<New<?>> NEW = new NewBinder();
	public static final ValueBinder<Produces<?>> PRODUCES = new ProducesBinder();
	public static final ValueBinder<Constant<?>> CONSTANT = new ConstantBinder();

	/**
	 * This {@link ValueBinder} adds bindings to super-types for
	 * {@link Binding}s declared with {@link DeclarationType#AUTO} or
	 * {@link DeclarationType#PROVIDED}.
	 */
	static final class SuperTypesBinder implements ValueBinder<Binding<?>> {

		@Override
		public <T> void expand(Env env, Binding<?> untyped, Binding<T> binding,
				Bindings bindings) {
			bindings.add(binding);
			DeclarationType declarationType = binding.source.declarationType;
			if (declarationType != DeclarationType.AUTO
				&& declarationType != DeclarationType.PROVIDED)
				return;
			for (Type<? super T> supertype : binding.type().supertypes())
				// Object is of course a superclass but not indented when doing auto-binds
				if (supertype.rawType != Object.class)
					bindings.add(binding.typed(supertype));
		}
	}

	static final class ArrayElementsBinder
			implements ValueBinder.Completion<Parameter<?>[]> {

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <T> Binding<T> complete(Binding<T> incomplete,
				Parameter<?>[] elements) {
			return incomplete.complete(PREDEFINED,
					supplier((Type) incomplete.type(), elements));
		}

		@SuppressWarnings("unchecked")
		static <E> Supplier<E> supplier(Type<E[]> array,
				Parameter<?>[] elements) {
			return (Supplier<E>) Supply.elements(array,
					(Parameter<? extends E>[]) elements);
		}

	}

	static final class NewBinder implements ValueBinder<New<?>> {

		@Override
		public <T> void expand(Env env, New<?> constructor,
				Binding<T> incomplete, Bindings bindings) {
			bindings.addExpanded(env, incomplete.complete(CONSTRUCTOR,
					constructor(constructor.typed(incomplete.type()))));
			Class<?> impl = constructor.target.getDeclaringClass();
			Source source = incomplete.source;
			bindings.addConstant(env, source.typed(MULTI), Name.DEFAULT,
					Constructor.class, constructor.target);
			plugAnnotationsInto(env, bindings, source, impl, ElementType.TYPE,
					impl);
			plugAnnotationsInto(env, bindings, source, impl,
					ElementType.CONSTRUCTOR, constructor.target);
		}
	}

	static final class ProducesBinder implements ValueBinder<Produces<?>> {

		@Override
		public <T> void expand(Env env, Produces<?> method,
				Binding<T> incomplete, Bindings bindings) {
			bindings.addExpanded(env, incomplete.complete(METHOD,
					method(method.typed(incomplete.type()))));
			Source source = incomplete.source;
			bindings.addConstant(env, source.typed(MULTI), Name.DEFAULT,
					Method.class, method.target);
			plugAnnotationsInto(env, bindings, source,
					method.target.getDeclaringClass(), ElementType.METHOD,
					method.target);
		}
	}

	static final class TypeParametrizedReferenceBinder
			implements ValueBinder.Completion<Class<?>> {

		@Override
		public <T> Binding<T> complete(Binding<T> incomplete, Class<?> to) {
			return incomplete.complete(REFERENCE, parametrizedInstance(
					anyOf(raw(to).castTo(incomplete.type()))));
		}

	}

	static final class ConstantBinder implements ValueBinder<Constant<?>> {

		@Override
		public <T> void expand(Env env, Constant<?> value,
				Binding<T> incomplete, Bindings bindings) {
			Supplier<T> supplier = Bindings.constantSupplier(
					incomplete.type().rawType.cast(value.value));
			bindings.addExpanded(env,
					incomplete.complete(BindingType.PREDEFINED, supplier));
			// implicitly bind to the exact type of the constant
			// should that differ from the binding type
			if (incomplete.source.declarationType == DeclarationType.EXPLICIT
				&& incomplete.type().rawType != value.value.getClass()) {
				@SuppressWarnings("unchecked")
				Class<T> type = (Class<T>) value.value.getClass();
				bindings.addExpanded(env, Binding.binding( //TODO maybe this should use addConstant? do we need the auto inheritance processing?
						incomplete.locator.typed(raw(type)),
						BindingType.PREDEFINED, supplier, incomplete.scope,
						incomplete.source.typed(DeclarationType.IMPLICIT)));
			}
		}
	}

	static final class ReferenceBinder implements ValueBinder<Instance<?>> {

		@Override
		public <T> void expand(Env env, Instance<?> linked, Binding<T> binding,
				Bindings bindings) {
			Type<?> t = linked.type();
			if (t.isAssignableTo(raw(Supplier.class))
				&& !binding.type().isAssignableTo(raw(Supplier.class))) {
				@SuppressWarnings("unchecked")
				Class<? extends Supplier<? extends T>> supplier = (Class<? extends Supplier<? extends T>>) t.rawType;
				bindings.addExpanded(env, binding.complete(REFERENCE,
						Supply.reference(supplier)));
				implicitlyBindToConstructor(env, binding, linked, bindings);
				return;
			}
			final Type<? extends T> type = t.castTo(binding.type());
			final Instance<T> bound = binding.locator.instance;
			if (!bound.type().equalTo(type)
				|| !linked.name.isCompatibleWith(bound.name)) {
				bindings.addExpanded(env, binding.complete(REFERENCE,
						Supply.instance(linked.typed(type))));
				implicitlyBindToConstructor(env, binding, linked, bindings);
				Class<T> boundRawType = bound.type().rawType;
				plugAnnotationsInto(env, bindings, binding.source, boundRawType,
						ElementType.TYPE, boundRawType);
				return;
			}
			if (type.isInterface())
				throw InconsistentBinding.loop(binding, linked, bound);
			bindToMirrorConstructor(env, bindings, binding, type.rawType);
		}

	}

	static <T> void implicitlyBindToConstructor(Env env, Binding<?> incomplete,
			Instance<T> instance, Bindings bindings) {
		Class<T> impl = instance.type().rawType;
		if (isClassInstantiable(impl)) {
			Binding<T> binding = Binding.binding(
					new Locator<>(instance).indirect(
							incomplete.locator.target.indirect),
					BindingType.CONSTRUCTOR, null, incomplete.scope,
					incomplete.source.typed(DeclarationType.IMPLICIT));
			bindToMirrorConstructor(env, bindings, binding, impl);
		}
	}

	static <T> void bindToMirrorConstructor(Env env, Bindings bindings,
			Binding<T> binding, Class<? extends T> impl) {
		Constructor<? extends T> target = env.property(ConstructsBy.class,
				binding.source.pkg()).reflect(impl);
		if (target != null)
			bindings.addExpanded(env, binding, New.bind(target));
	}

	static void plugAnnotationsInto(Env env, Bindings bindings, Source source,
			Class<?> plugin, ElementType declaredType,
			AnnotatedElement declaration) {
		Annotation[] annotations = declaration.getAnnotations();
		if (annotations.length > 0) {
			for (Annotation a : annotations)
				plugAnnotationInto(env, bindings, source, plugin, declaredType,
						a.annotationType());
		}
	}

	static void plugAnnotationInto(Env env, Bindings bindings, Source source,
			Class<?> plugin, ElementType declaredType,
			Class<? extends Annotation> pluginPoint) {
		bindings.addConstant(env, source.typed(DeclarationType.MULTI),
				pluginPoint(pluginPoint, declaredType.name()), Class.class,
				plugin);
	}
}
