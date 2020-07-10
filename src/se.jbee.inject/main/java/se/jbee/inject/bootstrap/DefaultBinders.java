/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.DeclarationType.MULTI;
import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.instance;
import static se.jbee.inject.Utils.isClassBanal;
import static se.jbee.inject.Utils.isClassInstantiable;
import static se.jbee.inject.bootstrap.Supply.constructor;
import static se.jbee.inject.bootstrap.Supply.method;
import static se.jbee.inject.bootstrap.Supply.parametrizedInstance;
import static se.jbee.inject.declare.BindingType.CONSTRUCTOR;
import static se.jbee.inject.declare.BindingType.METHOD;
import static se.jbee.inject.declare.BindingType.PREDEFINED;
import static se.jbee.inject.declare.BindingType.REFERENCE;
import static se.jbee.inject.declare.Bindings.supplyScopedConstant;
import static se.jbee.inject.declare.Bindings.supplyConstant;
import static se.jbee.inject.extend.Plugins.pluginPoint;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import se.jbee.inject.DeclarationType;
import se.jbee.inject.Env;
import se.jbee.inject.Instance;
import se.jbee.inject.Locator;
import se.jbee.inject.Parameter;
import se.jbee.inject.Source;
import se.jbee.inject.Type;
import se.jbee.inject.Utils;
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
			return (Supplier<E>) Supply.elements(array,
					(Parameter<? extends E>[]) elements);
		}

	}

	static final class NewBinder implements ValueBinder<New<?>> {

		@Override
		public <T> void expand(Env env, New<?> src, Binding<T> item,
				Bindings target) {
			target.addExpanded(env, item.complete(CONSTRUCTOR,
					constructor(src.typed(item.type()))));
			Class<?> impl = src.target.getDeclaringClass();
			Source source = item.source;
			target.addConstant(env, source.typed(MULTI),
					named(src.target.getName()), Constructor.class, src.target);
			plugAnnotationsInto(env, target, source, impl, ElementType.TYPE,
					impl);
			plugAnnotationsInto(env, target, source, impl,
					ElementType.CONSTRUCTOR, src.target);
		}
	}

	static final class TypeParametrizedReferenceBinder
			implements ValueBinder.Completion<Class<?>> {

		@Override
		public <T> Binding<T> complete(Binding<T> item, Class<?> to) {
			return item.complete(REFERENCE,
					parametrizedInstance(anyOf(raw(to).castTo(item.type()))));
		}

	}

	static final class ProducesBinder implements ValueBinder<Produces<?>> {

		@Override
		public <T> void expand(Env env, Produces<?> src, Binding<T> item,
				Bindings target) {
			target.addExpanded(env,
					item.complete(METHOD, method(src.typed(item.type()))));
			Source source = item.source;
			target.addConstant(env, source.typed(MULTI),
					named(src.target.getName()), Method.class, src.target);
			plugAnnotationsInto(env, target, source,
					src.target.getDeclaringClass(), ElementType.METHOD,
					src.target);
		}
	}

	static final class SharesBinder implements ValueBinder<Shares<?>> {

		@Override
		public <T> void expand(Env env, Shares<?> src, Binding<T> item,
				Bindings target) {
			Source source = item.source;
			target.addExpanded(env, item, new Constant<>(
					Utils.share(src.constant, src.owner)).manual());
			//TODO should the share be scoped or not?
			// There is not right answer => sometimes it might be intended, often it is not
			target.addConstant(env, source.typed(MULTI),
					named(src.constant.getName()), Field.class, src.constant);
			plugAnnotationsInto(env, target, source,
					src.constant.getDeclaringClass(), ElementType.FIELD,
					src.constant);
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
			plugAnnotationsInto(env, target, item.source, impl,
					ElementType.TYPE, impl);
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
				target.addExpanded(env,
						item.complete(REFERENCE, Supply.reference(supplier)));
				implicitlyBindToConstructor(env, src, item, target);
				return;
			}
			final Type<? extends T> type = srcType.castTo(item.type());
			final Instance<T> bound = item.locator.instance;
			if (!bound.type().equalTo(type)
				|| !src.name.isCompatibleWith(bound.name)) {
				target.addExpanded(env, item.complete(REFERENCE,
						Supply.instance(src.typed(type))));
				implicitlyBindToConstructor(env, src, item, target);
				Class<T> boundRawType = bound.type().rawType;
				plugAnnotationsInto(env, target, item.source, boundRawType,
						ElementType.TYPE, boundRawType);
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

	static void plugAnnotationsInto(Env env, Bindings target, Source source,
			Class<?> plugin, ElementType declaredType,
			AnnotatedElement declaration) {
		Annotation[] annotations = declaration.getAnnotations();
		if (annotations.length > 0) {
			for (Annotation a : annotations)
				plugAnnotationInto(env, target, source, plugin, declaredType,
						a.annotationType());
		}
	}

	static void plugAnnotationInto(Env env, Bindings target, Source source,
			Class<?> plugin, ElementType declaredType,
			Class<? extends Annotation> pluginPoint) {
		target.addConstant(env, source.typed(DeclarationType.MULTI),
				pluginPoint(pluginPoint, declaredType.name()), Class.class,
				plugin);
	}
}
