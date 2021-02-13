/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.defaults;

import se.jbee.inject.*;
import se.jbee.inject.bind.Bindings;
import se.jbee.inject.bind.Bootstrapper;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.Dependent;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Supply;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.*;
import se.jbee.lang.Lazy;
import se.jbee.lang.Type;
import se.jbee.lang.Utils;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

import static se.jbee.inject.Resource.resourcesTypeOf;
import static se.jbee.inject.Scope.application;
import static se.jbee.lang.Cast.functionTypeOf;
import static se.jbee.lang.Type.raw;

/**
 * Installs all the build-in functionality by using the core API.
 */
public enum DefaultFeature implements Dependent<DefaultFeature> {
	/**
	 * Adds: {@link Provider}s can be injected for all bound types.
	 */
	PROVIDER(false),
	/**
	 * Adds: {@link List}s can be injected for all bound types (via array
	 * bridge)
	 */
	LIST(false),
	/**
	 * Adds: {@link Set} can be injected for all bound types (via array bridge)
	 */
	SET(false),
	/**
	 * Adds: {@link Collection} can be injected instead of {@link List} (needs
	 * explicit List bind).
	 */
	COLLECTION(false),
	/**
	 * Adds: {@link Logger}s can be injected per receiving class.
	 */
	LOGGER(false),
	/**
	 * Adds: Support for injection of {@link Optional}s. If {@link Optional}
	 * parameters cannot be resolved {@link Optional#empty()} is injected.
	 */
	OPTIONAL(false),
	/**
	 * Adds: That primitive arrays can be resolved for their wrapper types.
	 *
	 * Note that this only supports one-dimensional arrays of int, long, float,
	 * double and boolean. For further support add similar suppliers following
	 * the example given.
	 */
	PRIMITIVE_ARRAYS(false),
	/**
	 * Adds: Support for {@link Injector} sub-contexts.
	 */
	SUB_CONTEXT(true),
	/**
	 * Adds: Binds the bootstrapping {@link Env} as the {@link Name#DEFAULT}
	 * {@link Env} in the {@link Injector} context.
	 */
	ENV(true),
	/**
	 * Adds: The {@link DefaultScopes}
	 */
	SCOPES(true),
	/**
	 * Adds: {@link Extension}s via {@link ExtensionModule}.
	 */
	EXTENSION(true),
	/**
	 * Adds: {@link AnnotatedWith} via {@link AnnotatedWithModule}.
	 */
	ANNOTATED_WITH(true),
	/**
	 * Adds: {@link Obtainable}s
	 */
	OBTAINABLE(true),

	/**
	 * Adds: injection {@link Type}, {@link Name} and {@link Dependency}.
	 */
	SELF(true),

	/**
	 * Adds: Default bindings for {@link New}, {@link Invoke} and {@link Get}.
	 */
	REFLECT(true),
	;

	public static final DefaultFeature[] INSTALLED_BY_DEFAULT = Arrays.stream(DefaultFeature.values()) //
			.filter(e -> e.installedByDefault) //
			.toArray(DefaultFeature[]::new);

	public final boolean installedByDefault;

	DefaultFeature(boolean installedByDefault) {
		this.installedByDefault = installedByDefault;
	}

	@Override
	public void bootstrap(
			Bootstrapper.DependentBootstrapper<DefaultFeature> bootstrapper) {
		bootstrapper.installDependentOn(LIST, ListBridgeModule.class);
		bootstrapper.installDependentOn(SET, SetBridgeModule.class);
		bootstrapper.installDependentOn(COLLECTION, CollectionBridgeModule.class);
		bootstrapper.installDependentOn(PROVIDER, ProviderBridgeModule.class);
		bootstrapper.installDependentOn(LOGGER, LoggerModule.class);
		bootstrapper.installDependentOn(OPTIONAL, OptionalBridgeModule.class);
		bootstrapper.installDependentOn(SUB_CONTEXT, SubContextModule.class);
		bootstrapper.installDependentOn(ENV, DefaultEnvModule.class);
		bootstrapper.installDependentOn(SCOPES, DefaultScopes.class);
		bootstrapper.installDependentOn(EXTENSION, ExtensionModule.class);
		bootstrapper.installDependentOn(PRIMITIVE_ARRAYS, PrimitiveArraysModule.class);
		bootstrapper.installDependentOn(ANNOTATED_WITH, AnnotatedWithModule.class);
		bootstrapper.installDependentOn(OBTAINABLE, ObtainableModule.class);
		bootstrapper.installDependentOn(SELF, SelfModule.class);
		bootstrapper.installDependentOn(REFLECT, ReflectModule.class);
	}

	private static class ReflectModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault().bind(New.class).to(Constructor::newInstance);
			asDefault().bind(Invoke.class).to(Method::invoke);
			asDefault().bind(Get.class).to(Field::get);
		}
	}

	private static class LoggerModule extends BinderModule {

		private static final Supplier<Logger> LOGGER = (dep, context) //
				-> Logger.getLogger(dep.target(1).type().rawType.getCanonicalName());

		@Override
		protected void declare() {
			per(Scope.targetInstance)//
					.starbind(Logger.class) //
					.toSupplier(LOGGER);
		}

	}

	private static class ProviderBridgeModule extends BinderModule {

		@Override
		protected void declare() {
			per(Scope.dependency)//
					.starbind(Provider.class) //
					.toSupplier(Supply.PROVIDER);
		}

	}

	private static class ListBridgeModule extends BinderModule {

		@Override
		protected void declare() {
			per(Scope.dependency)//
					.starbind(List.class) //
					.toSupplier(Supply.LIST_BRIDGE);
		}

	}

	private static class SetBridgeModule extends BinderModule {

		@Override
		protected void declare() {
			per(Scope.dependency)//
					.starbind(Set.class) //
					.toSupplier(Supply.SET_BRIDGE);
		}

	}

	private static class CollectionBridgeModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault() //
					.per(Scope.dependency) //
					.starbind(Collection.class) //
					.toParametrized(List.class);
		}
	}

	private static class OptionalBridgeModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault() //
					.per(Scope.dependency) //
					.starbind(Optional.class) //
					.toSupplier(this::optional);
		}

		@SuppressWarnings("unchecked")
		<T> Optional<T> optional(Dependency<? super Optional<T>> dep, Injector context) {
			try {
				return Optional.ofNullable(
						(T) context.resolve(dep.onTypeParameter().uninject()));
			} catch (UnresolvableDependency e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * Provides {@link Injector} sub-contexts on the basis of {@link Plugins}s.
	 * When installing a {@link Bundle} in a sub-context this simply plugs that
	 * {@link Bundle} into the point of the sub-context which is {@link
	 * Injector} and the sub-context {@link Name}.
	 * <p>
	 * Extracting the {@link Bundle}s via {@link Plugins} is one part. The
	 * creation of the {@link Injector} from those {@link Bundle}s is extracted
	 * into a separate {@link Function} so this can be replaced independently.
	 */
	private static final class SubContextModule extends BinderModule
			implements se.jbee.inject.Supplier<Injector>, Injector {

		@Override
		protected void declare() {
			asDefault().bind(functionTypeOf(Class[].class, Injector.class)) //
					.to(roots -> createSubContextFromRootBundles(env().in(null), roots));
			asDefault() //
					.per(Scope.dependencyInstance) //
					.starbind(Injector.class) //
					.toSupplier(this);
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private Injector createSubContextFromRootBundles(Env env, Class[] roots) {
			return Bootstrap.injector(env, Bindings.newBindings(), roots);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Injector supply(Dependency<? super Injector> dep,
				Injector context) throws UnresolvableDependency {
			@SuppressWarnings("unchecked")
			Class<? extends Bundle>[] bundles = (Class<? extends Bundle>[]) //
			context.resolve(Plugins.class).forPoint(Injector.class,
					dep.instance.name.toString());
			if (bundles.length == 0)
				return this; // this module acts as an Injector that directly fails to resolve any Dependency
			return context.resolve(functionTypeOf(Class[].class, Injector.class)).apply(bundles);
		}

		@Override
		public <T> T resolve(Dependency<T> dep) throws UnresolvableDependency {
			throw new UnresolvableDependency.ResourceResolutionFailed(
					"Empty SubContext Injector", dep);
		}
	}

	private static final class ObtainableModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault() //
					.per(Scope.dependency) //
					.starbind(Obtainable.class) //
					.toSupplier(this::obtain);
		}

		@SuppressWarnings("unchecked")
		<T, E> Obtainable<T> obtain(Dependency<? super Obtainable<T>> dep, Injector context) {
			Dependency<T> targetDep = (Dependency<T>) dep.onTypeParameter().uninject();
			Type<T> targetType = targetDep.type();
			if (targetType.arrayDimensions() == 1) {
				Dependency<E> elementDep = (Dependency<E>) dep.typed(targetType.baseType());
				return new ObtainableCollection<>(context, elementDep);
			}
			return new ObtainableInstance<>(() -> context.resolve(targetDep));
		}

		static final class ObtainableCollection<T, E> implements Obtainable<T> {

			private final Injector context;
			private final Dependency<E> dep;
			private final Lazy<T> value = new Lazy<>();

			ObtainableCollection(Injector context, Dependency<E> dep) {
				this.context = context;
				this.dep = dep;
			}

			@SuppressWarnings("unchecked")
			private T resolve() {
				Resource<E>[] resources = context.resolve(
						dep.typed(resourcesTypeOf(dep.type())));
				List<E> elements = new ArrayList<>();
				for (Resource<E> r : resources) {
					try {
						elements.add(r.generate(dep));
					} catch (UnresolvableDependency e) {
						// ignored
					}
				}
				return (T) Utils.arrayOf(elements, dep.type().rawType);
			}

			@Override
			public T obtain() {
				return value.get(this::resolve);
			}
		}

		static final class ObtainableInstance<T> implements Obtainable<T> {

			private final Provider<T> resolver;
			private final Lazy<T> value = new Lazy<>();
			private UnresolvableDependency caught;

			ObtainableInstance(Provider<T> resolver) {
				this.resolver = resolver;
			}

			private T resolve() {
				try {
					return resolver.provide();
				} catch (UnresolvableDependency e) {
					caught = e;
					return null;
				}
			}

			@Override
			public T obtain() {
				return value.get(this::resolve);
			}

			@Override
			public <X extends Exception> T orElseThrow(
					Function<UnresolvableDependency, ? extends X> exceptionTransformer)
					throws X {
				T res = obtain();
				if (res != null)
					return res;
				throw exceptionTransformer.apply(caught);
			}
		}
	}

	private static final class DefaultEnvModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault().bind(Env.class).to(env().in(null));
		}
	}

	private static final class SelfModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault().per(Scope.dependency) //
					.starbind(Type.class) //
					.toSupplier(SelfModule::supplyType);
			asDefault().per(Scope.dependency) //
					.starbind(Name.class) //
					.toSupplier(SelfModule::supplyName);
			asDefault().per(Scope.dependency) //
					.starbind(Dependency.class) //
					.toSupplier(SelfModule::supplyDependency);
		}

		private static Type<?> supplyType(Dependency<? super Type<?>> dep,
				Injector context) {
			return dep.injection(findTypeInjectionFrame(dep)).dependency.type;
		}

		private static Name supplyName(Dependency<? super Name> dep,
				Injector context) {
			Injection injected = dep.injection(1);
			return injected.dependency.name.isAny()
				   ? injected.target.instance.name
				   : injected.dependency.name;
		}

		private static Dependency<?> supplyDependency(
				Dependency<? super Dependency<?>> dep, Injector context) {
			return dep.onTypeParameter().uninject();
		}

		private static int findTypeInjectionFrame(Dependency<?> dep) {
			Type<?> target = dep.injection(1).dependency.type;
			if (!target.isRawType())
				return 1;
			for (int frame = 2; frame < dep.injectionDepth(); frame++) {
				Type<?> candidate = dep.injection(frame).dependency.type;
				if (candidate.rawType == target.rawType && !candidate.isRawType())
					return frame;
			}
			return 1;
		}
	}

	private static final class PrimitiveArraysModule extends BinderModule {

		@Override
		protected void declare() {
			ScopedBinder asDefault = asDefault().per(application);
			asDefault.bind(int[].class) //
					.toSupplier(PrimitiveArraysModule::ints);
			asDefault.bind(long[].class) //
					.toSupplier(PrimitiveArraysModule::longs);
			asDefault.bind(float[].class) //
					.toSupplier(PrimitiveArraysModule::floats);
			asDefault.bind(double[].class) //
					.toSupplier(PrimitiveArraysModule::doubles);
			asDefault.bind(boolean[].class) //
					.toSupplier(PrimitiveArraysModule::booleans);
		}

		private static <T> T copyToPrimitiveArray(Object[] src, T dest) {
			for (int i = 0; i < src.length; i++)
				Array.set(dest, i, src[i]);
			return dest;
		}

		private static int[] ints(Dependency<? super int[]> dep,
				Injector context) {
			Integer[] wrappers = context.resolve(
					dep.typed(raw(Integer[].class)));
			return copyToPrimitiveArray(wrappers, new int[wrappers.length]);
		}

		public static long[] longs(Dependency<? super long[]> dep,
				Injector context) {
			Long[] wrappers = context.resolve(dep.typed(raw(Long[].class)));
			return copyToPrimitiveArray(wrappers, new long[wrappers.length]);
		}

		public static float[] floats(Dependency<? super float[]> dep,
				Injector context) {
			Float[] wrappers = context.resolve(dep.typed(raw(Float[].class)));
			return copyToPrimitiveArray(wrappers, new float[wrappers.length]);
		}

		public static double[] doubles(Dependency<? super double[]> dep,
				Injector context) {
			Double[] wrappers = context.resolve(dep.typed(raw(Double[].class)));
			return copyToPrimitiveArray(wrappers, new double[wrappers.length]);
		}

		public static boolean[] booleans(Dependency<? super boolean[]> dep,
				Injector context) {
			Boolean[] wrappers = context.resolve(
					dep.typed(raw(Boolean[].class)));
			return copyToPrimitiveArray(wrappers, new boolean[wrappers.length]);
		}
	}
}
