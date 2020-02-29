/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.DeclarationType.MULTI;
import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.arrayIndex;
import static se.jbee.inject.Utils.arrayPrepand;
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
import se.jbee.inject.InconsistentDeclaration;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Parameter;
import se.jbee.inject.Resource;
import se.jbee.inject.Source;
import se.jbee.inject.Type;
import se.jbee.inject.container.Supplier;

/**
 * A immutable collection of {@link Macro}s each bound to a specific type
 * handled.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Macros {

	// TODO can Macros be Options?

	public static final Macro<Binding<?>> EXPAND = new AutoInheritanceMacro();
	public static final Macro<Class<?>> PARAMETRIZED_REF = new TypeParametrizedReferenceMacro();
	public static final Macro<Instance<?>> INSTANCE_REF = new ReferenceMacro();
	public static final Macro<Parameter<?>[]> ARRAY = new ArrayElementsMacro();
	public static final Macro<New<?>> NEW = new NewMacro();
	public static final Macro<Factory<?>> FACTORY = new FactoryMacro();
	public static final Macro<Constant<?>> CONSTANT = new ConstantMacro();

	public static final Macros NONE = new Macros(new Class<?>[0],
			new Macro<?>[0]);

	public static final Macros DEFAULT = Macros.NONE //
			.with(EXPAND) //
			.with(NEW) //
			.with(CONSTANT)//
			.with(FACTORY) //
			.with(INSTANCE_REF) //
			.with(PARAMETRIZED_REF) //
			.with(ARRAY);

	private final Class<?>[] types;
	private final Macro<?>[] functions;

	private Macros(Class<?>[] types, Macro<?>[] functions) {
		this.types = types;
		this.functions = functions;
	}

	/**
	 * Uses the given {@link Macro} and derives the {@link #with(Class, Macro)}
	 * type from its declaration. This is a utility method that can be used as
	 * long as the {@link Macro} implementation is not generic.
	 *
	 * @param macro No generic macro class (e.g. decorators)
	 * @return A set of {@link Macros} containing the given one for the type
	 *         derived from its type declaration.
	 */
	@SuppressWarnings("unchecked")
	public <T> Macros with(Macro<T> macro) {
		Class<?> type = Type.supertype(Macro.class,
				Type.raw(macro.getClass())).parameter(0).rawType;
		return with((Class<? super T>) type, macro);
	}

	/**
	 * Uses the given {@link Macro} for the given exact (no super-types!) type
	 * of values.
	 *
	 * @param type The type of value that should be passed to the {@link Macro}
	 *            as value
	 * @param macro The {@link Macro} expanding the type of value
	 * @return A set of {@link Macros} containing the given one
	 */
	public <T> Macros with(Class<T> type, Macro<? extends T> macro) {
		int index = index(type);
		if (index >= 0) {
			Macro<?>[] tmp = functions.clone();
			tmp[index] = macro;
			return new Macros(types, tmp);
		}
		return new Macros(arrayPrepand(type, types),
				arrayPrepand(macro, functions));
	}

	/**
	 * A generic version of {@link Macro#expand(Object, Binding, Bindings)} that
	 * uses the matching predefined {@link Macro} for the actual type of the
	 * value and expands it.
	 *
	 * @param binding The usually incomplete binding to expand (and add to
	 *            {@link Bindings})
	 * @param value Non-null value to expand via matching {@link Macro}
	 *
	 * @throws InconsistentDeclaration In case no {@link Macro} had been
	 *             declared for the type of value argument
	 */
	public <T, V> void expandInto(Bindings bindings, Binding<T> binding,
			V value) {
		macroForValueOf(value.getClass(), binding).expand(value, binding,
				bindings);
	}

	@SuppressWarnings("unchecked")
	private <V> Macro<? super V> macroForValueOf(final Class<?> type,
			Binding<?> binding) {
		int index = index(type);
		if (index < 0)
			throw InconsistentBinding.undefinedMacroType(binding, type);
		return (Macro<? super V>) functions[index];
	}

	private int index(final Class<?> type) {
		return arrayIndex(types, type, (a, b) -> a == b);
	}

	/**
	 * This {@link Macro} adds bindings to super-types for {@link Binding}s
	 * declared with {@link DeclarationType#AUTO} or
	 * {@link DeclarationType#PROVIDED}.
	 */
	static final class AutoInheritanceMacro implements Macro<Binding<?>> {

		@Override
		public <T> void expand(Binding<?> untyped, Binding<T> binding,
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
		public <T> void expand(New<?> constructor, Binding<T> incomplete,
				Bindings bindings) {
			bindings.addExpanded(incomplete.complete(CONSTRUCTOR,
					constructor(constructor.typed(incomplete.type()))));
			Class<?> impl = constructor.target.getDeclaringClass();
			Source source = incomplete.source;
			bindings.addConstant(source.typed(MULTI), Name.DEFAULT,
					Constructor.class, constructor.target);
			plugAnnotationsInto(bindings, source, impl, ElementType.TYPE, impl);
			plugAnnotationsInto(bindings, source, impl, ElementType.CONSTRUCTOR,
					constructor.target);
		}
	}

	static final class FactoryMacro implements Macro<Factory<?>> {

		@Override
		public <T> void expand(Factory<?> method, Binding<T> incomplete,
				Bindings bindings) {
			bindings.addExpanded(incomplete.complete(METHOD,
					method(method.typed(incomplete.type()))));
			Source source = incomplete.source;
			bindings.addConstant(source.typed(MULTI), Name.DEFAULT,
					Method.class, method.target);
			plugAnnotationsInto(bindings, source,
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
		public <T> void expand(Constant<?> value, Binding<T> incomplete,
				Bindings bindings) {
			Supplier<T> supplier = Supply.constant(
					incomplete.type().rawType.cast(value.value));
			bindings.addExpanded(
					incomplete.complete(BindingType.PREDEFINED, supplier));
			// implicitly bind to the exact type of the constant
			// should that differ from the binding type
			if (incomplete.source.declarationType == DeclarationType.EXPLICIT
				&& incomplete.type().rawType != value.value.getClass()) {
				@SuppressWarnings("unchecked")
				Class<T> type = (Class<T>) value.value.getClass();
				bindings.addExpanded(Binding.binding(
						incomplete.resource.typed(raw(type)),
						BindingType.PREDEFINED, supplier, incomplete.scope,
						incomplete.source.typed(DeclarationType.IMPLICIT)));
			}
		}
	}

	static final class ReferenceMacro implements Macro<Instance<?>> {

		@Override
		public <T> void expand(Instance<?> linked, Binding<T> binding,
				Bindings bindings) {
			Type<?> t = linked.type();
			if (t.isAssignableTo(raw(Supplier.class))
				&& !binding.type().isAssignableTo(raw(Supplier.class))) {
				@SuppressWarnings("unchecked")
				Class<? extends Supplier<? extends T>> supplier = (Class<? extends Supplier<? extends T>>) t.rawType;
				bindings.addExpanded(binding.complete(REFERENCE,
						Supply.reference(supplier)));
				implicitlyBindToConstructor(binding, linked, bindings);
				return;
			}
			final Type<? extends T> type = t.castTo(binding.type());
			final Instance<T> bound = binding.resource.instance;
			if (!bound.type().equalTo(type)
				|| !linked.name.isCompatibleWith(bound.name)) {
				bindings.addExpanded(binding.complete(REFERENCE,
						Supply.instance(linked.typed(type))));
				implicitlyBindToConstructor(binding, linked, bindings);
				Class<T> boundRawType = bound.type().rawType;
				plugAnnotationsInto(bindings, binding.source, boundRawType,
						ElementType.TYPE, boundRawType);
				return;
			}
			if (type.isInterface())
				throw InconsistentBinding.loop(binding, linked, bound);
			bindToMirrorConstructor(bindings, binding, type.rawType);
		}

	}

	static <T> void implicitlyBindToConstructor(Binding<?> incomplete,
			Instance<T> instance, Bindings bindings) {
		Class<T> impl = instance.type().rawType;
		if (isClassInstantiable(impl)) {
			Binding<T> binding = Binding.binding(
					new Resource<>(instance).indirect(
							incomplete.resource.target.indirect),
					BindingType.CONSTRUCTOR, null, incomplete.scope,
					incomplete.source.typed(DeclarationType.IMPLICIT));
			bindToMirrorConstructor(bindings, binding, impl);
		}
	}

	static <T> void bindToMirrorConstructor(Bindings bindings,
			Binding<T> binding, Class<? extends T> impl) {
		Constructor<? extends T> target = bindings.mirrors.construction.reflect(
				impl);
		if (target != null)
			bindings.macros.expandInto(bindings, binding, New.bind(target));
	}

	static void plugAnnotationsInto(Bindings bindings, Source source,
			Class<?> plugin, ElementType declaredType,
			AnnotatedElement declaration) {
		Annotation[] annotations = declaration.getAnnotations();
		if (annotations.length > 0) {
			for (Annotation a : annotations)
				plugAnnotationInto(bindings, source, plugin, declaredType,
						a.annotationType());
		}
	}

	static void plugAnnotationInto(Bindings bindings, Source source,
			Class<?> plugin, ElementType declaredType,
			Class<? extends Annotation> pluginPoint) {
		bindings.addConstant(source.typed(DeclarationType.MULTI),
				pluginPoint(pluginPoint, declaredType.name()), Class.class,
				plugin);
	}
}
