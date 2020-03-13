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
import static se.jbee.inject.bootstrap.BindingType.CONSTRUCTOR;
import static se.jbee.inject.bootstrap.BindingType.METHOD;
import static se.jbee.inject.bootstrap.BindingType.PREDEFINED;
import static se.jbee.inject.bootstrap.BindingType.REFERENCE;
import static se.jbee.inject.bootstrap.Supply.constructor;
import static se.jbee.inject.bootstrap.Supply.method;
import static se.jbee.inject.bootstrap.Supply.parametrizedInstance;
import static se.jbee.inject.config.Plugins.pluginPoint;

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
import se.jbee.inject.declare.Macro;

/**
 * A immutable collection of {@link Macro}s each bound to a specific type
 * handled.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Macros {

	public static final Macro<Binding<?>> EXPAND = new AutoInheritanceMacro();
	public static final Macro<Class<?>> PARAMETRIZED_REF = new TypeParametrizedReferenceMacro();
	public static final Macro<Instance<?>> INSTANCE_REF = new ReferenceMacro();
	public static final Macro<Parameter<?>[]> ARRAY = new ArrayElementsMacro();
	public static final Macro<New<?>> NEW = new NewMacro();
	public static final Macro<Produces<?>> PRODUCES = new ProducesMacro();
	public static final Macro<Constant<?>> CONSTANT = new ConstantMacro();

	/**
	 * This {@link Macro} adds bindings to super-types for {@link Binding}s
	 * declared with {@link DeclarationType#AUTO} or
	 * {@link DeclarationType#PROVIDED}.
	 */
	static final class AutoInheritanceMacro implements Macro<Binding<?>> {

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

	static final class ArrayElementsMacro
			implements Macro.Completion<Parameter<?>[]> {

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

	static final class NewMacro implements Macro<New<?>> {

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

	static final class ProducesMacro implements Macro<Produces<?>> {

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

	static final class TypeParametrizedReferenceMacro
			implements Macro.Completion<Class<?>> {

		@Override
		public <T> Binding<T> complete(Binding<T> incomplete, Class<?> to) {
			return incomplete.complete(REFERENCE, parametrizedInstance(
					anyOf(raw(to).castTo(incomplete.type()))));
		}

	}

	static final class ConstantMacro implements Macro<Constant<?>> {

		@Override
		public <T> void expand(Env env, Constant<?> value,
				Binding<T> incomplete, Bindings bindings) {
			Supplier<T> supplier = Supply.constant(
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

	static final class ReferenceMacro implements Macro<Instance<?>> {

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
