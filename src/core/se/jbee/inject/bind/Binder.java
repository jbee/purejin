/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Source.source;
import static se.jbee.inject.Target.targeting;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.isClassVirtual;
import static se.jbee.inject.Utils.newArray;
import static se.jbee.inject.config.Plugins.pluginPoint;
import static se.jbee.inject.container.Cast.initialiserTypeOf;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.BiConsumer;

import se.jbee.inject.Dependency;
import se.jbee.inject.Generator;
import se.jbee.inject.InconsistentDeclaration;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Packages;
import se.jbee.inject.Parameter;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Target;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bootstrap.Binding;
import se.jbee.inject.bootstrap.BindingType;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Bundle;
import se.jbee.inject.bootstrap.Constant;
import se.jbee.inject.bootstrap.Factory;
import se.jbee.inject.bootstrap.New;
import se.jbee.inject.bootstrap.Supply;
import se.jbee.inject.config.Config;
import se.jbee.inject.config.Globals;
import se.jbee.inject.config.Mirrors;
import se.jbee.inject.config.Plugins;
import se.jbee.inject.container.Initialiser;
import se.jbee.inject.container.Supplier;

/**
 * The default implementation of a fluent binder interface that provides a lot
 * of utility methods to improve readability and keep binding compact.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
@SuppressWarnings({ "squid:S1448", "squid:S1200" })
public class Binder {

	public static RootBinder create(Bind bind) {
		return new RootBinder(bind);
	}

	final RootBinder root;
	private final Bind bind;

	Binder(RootBinder root, Bind bind) {
		this.root = root == null ? (RootBinder) this : root;
		this.bind = bind.source == null ? bind.with(source(getClass())) : bind;
	}

	Bind bind() {
		return bind; // !ATTENTION! This method might be overridden to update
					// Bind properties - do not access field directly
	}

	protected final Bindings bindings() {
		return bind().bindings;
	}

	public void annotated(Class<?>... types) {
		Bindings bindings = bindings();
		for (Class<?> type : types)
			bindings.addFromAnnotated(type);
	}

	/**
	 * Allows access only via interface.
	 * 
	 * @since 19.1
	 */
	public Binder withIndirectAccess() {
		return with(bind().target.indirect());
	}

	public <E> TypedElementBinder<E> arraybind(Class<E[]> type) {
		return new TypedElementBinder<>(bind(), defaultInstanceOf(raw(type)));
	}

	public <T> TypedBinder<T> autobind(Class<T> type) {
		return autobind(Type.raw(type));
	}

	public <T> TypedBinder<T> autobind(Type<T> type) {
		return on(bind().asAuto()).bind(type);
	}

	public <T> TypedBinder<T> bind(Class<T> type) {
		return bind(Type.raw(type));
	}

	public <T> TypedBinder<T> bind(Instance<T> instance) {
		return new TypedBinder<>(bind(), instance);
	}

	public <T> TypedBinder<T> bind(Name name, Class<T> type) {
		return bind(name, Type.raw(type));
	}

	public <T> TypedBinder<T> bind(Name name, Type<T> type) {
		return bind(instance(name, type));
	}

	public <T> TypedBinder<T> bind(Type<T> type) {
		return bind(defaultInstanceOf(type));
	}

	public void construct(Class<?> type) {
		construct((defaultInstanceOf(raw(type))));
	}

	public void construct(Instance<?> instance) {
		bind(instance).toConstructor();
	}

	public void construct(Name name, Class<?> type) {
		construct(instance(name, raw(type)));
	}

	/**
	 * Bind something that is an {@link Initialiser} for the {@link Injector}.
	 * 
	 * @since 19.1
	 */
	public TypedBinder<Initialiser<Injector>> initbind() {
		return initbind(Injector.class);
	}

	/**
	 * @since 19.1
	 */
	public <T> TypedBinder<Initialiser<T>> initbind(Class<T> type) {
		return initbind(raw(type));
	}

	/**
	 * @since 19.1
	 */
	public <T> TypedBinder<Initialiser<T>> initbind(Type<T> type) {
		return multibind(initialiserTypeOf(type));
	}

	public <T> TypedBinder<T> multibind(Class<T> type) {
		return multibind(raw(type));
	}

	public <T> TypedBinder<T> multibind(Instance<T> instance) {
		return on(bind().asMulti()).bind(instance);
	}

	public <T> TypedBinder<T> multibind(Name name, Class<T> type) {
		return multibind(instance(name, raw(type)));
	}

	public <T> TypedBinder<T> multibind(Name name, Type<T> type) {
		return multibind(instance(name, type));
	}

	public <T> TypedBinder<T> multibind(Type<T> type) {
		return multibind(defaultInstanceOf(type));
	}

	public <T> TypedBinder<T> starbind(Class<T> type) {
		return bind(anyOf(raw(type)));
	}

	public <T> PluginBinder<T> plug(Class<T> plugin) {
		return new PluginBinder<>(on(bind()), plugin);
	}

	protected Binder on(Bind bind) {
		return new Binder(root, bind);
	}

	protected final Binder implicit() {
		return on(bind().asImplicit());
	}

	protected Binder with(Target target) {
		return new Binder(root, bind().with(target));
	}

	/**
	 * @see #lazyInstall(Class, String)
	 * @since 19.1
	 */
	public void lazyInstall(Class<? extends Bundle> bundle,
			Class<?> subContext) {
		lazyInstall(bundle, subContext.getName());
	}

	/**
	 * Binds a lazy {@link Bundle} which is installed in a sub-context
	 * {@link Injector}. The {@link Injector} is bootstrapped lazily on first
	 * usage. Use {@link Injector#subContext(String)} to resolve the sub-context
	 * by name.
	 * 
	 * @since 19.1
	 * @param bundle the {@link Bundle} to install in the sub-context lazy
	 *            {@link Injector}
	 * @param subContext the name of the lazy {@link Injector} sub-context
	 */
	public void lazyInstall(Class<? extends Bundle> bundle, String subContext) {
		plug(bundle).into(Injector.class, subContext);
		implicit().bind(Name.named(subContext), Injector.class).toSupplier(
				Binder::lazyInjector);
	}

	protected final static Injector lazyInjector(
			Dependency<? super Injector> dep, Injector context) {
		@SuppressWarnings("unchecked")
		Class<? extends Bundle>[] bundles = (Class<? extends Bundle>[]) //
		context.resolve(Plugins.class).forPoint(Injector.class,
				dep.instance.name.toString());
		return Bootstrap.injector(Bindings.newBindings(), Globals.STANDARD,
				bundles);
	}

	/**
	 * 
	 * @param target the type whose instances should be initialised by calling
	 *            some method
	 * @since 19.1
	 */
	public <T> InitBinder<T> init(Class<T> target) {
		return init(Name.DEFAULT, raw(target));
	}

	public <T> InitBinder<T> init(Name name, Type<T> target) {
		return new InitBinder<>(on(bind()), instance(name, target));
	}

	/**
	 * Small utility to make initialisation calls that depend on instances
	 * managed by the {@link Injector} easier.
	 * 
	 * The basic principle is that the {@link #target} {@link Instance} is
	 * initialised on the basis of some other dependency instance that is
	 * resolved during initialisation phase and provided to the
	 * {@link BiConsumer} function.
	 * 
	 * @param <T> type of the instances that should be initialised
	 * 
	 * @since 19.1
	 */
	public static class InitBinder<T> {

		private final Binder binder;
		private final Instance<T> target;

		protected InitBinder(Binder binder, Instance<T> target) {
			this.binder = binder;
			this.target = target;
		}

		public <C> void forAny(Class<? extends C> dependency,
				BiConsumer<T, C> initialiser) {
			forEach(raw(dependency).addArrayDimension().asUpperBound(),
					initialiser);
		}

		public <C> void forEach(Type<? extends C[]> dependencies,
				BiConsumer<T, C> initialiser) {
			binder.initbind().to((impl, injector) -> {
				T obj = injector.resolve(target);
				C[] args = injector.resolve(
						dependency(dependencies).injectingInto(target));
				for (C arg : args)
					initialiser.accept(obj, arg);
				return impl;
			});
		}

		public <C> void by(Class<? extends C> depencency,
				BiConsumer<T, C> initialiser) {
			by(defaultInstanceOf(raw(depencency)), initialiser);
		}

		public <C> void by(Name depName, Class<? extends C> depType,
				BiConsumer<T, C> initialiser) {
			by(Instance.instance(depName, raw(depType)), initialiser);
		}

		public <C> void by(Instance<? extends C> dependency,
				BiConsumer<T, C> initialiser) {
			binder.initbind().to((impl, injector) -> {
				T obj = injector.resolve(target);
				C arg = injector.resolve(
						dependency(dependency).injectingInto(target));
				initialiser.accept(obj, arg);
				return impl;
			});
		}
	}

	public static class PluginBinder<T> {

		private final Binder binder;
		private final Class<T> plugin;

		protected PluginBinder(Binder binder, Class<T> plugin) {
			this.binder = binder;
			this.plugin = plugin;
		}

		public void into(Class<?> pluginPoint) {
			into(pluginPoint, plugin.getCanonicalName());
		}

		public void into(Class<?> pluginPoint, String property) {
			binder.multibind(pluginPoint(pluginPoint, property),
					Class.class).to(plugin);
			if (!isClassVirtual(plugin))
				binder.implicit().construct(plugin);
			// we allow both collections of classes that have a common
			// super-type or collections that don't
			if (raw(plugin).isAssignableTo(raw(pluginPoint).asUpperBound())
				&& !plugin.isAnnotation()) {
				// if they have a common super-type the plugin is bound as an
				// implementation
				@SuppressWarnings("unchecked")
				Class<? super T> pp = (Class<? super T>) pluginPoint;
				binder.multibind(pp).to(plugin);
			}
		}
	}

	/**
	 * The {@link AutoBinder} makes use of the reflectors defined in
	 * {@link Bindings} to select and bind constructors for beans and methods as
	 * factories and {@link Name} these instances as well as provide
	 * {@link Parameter} hints.
	 * 
	 * @since 19.1
	 */
	public static class AutoBinder {

		private final ScopedBinder binder;

		protected AutoBinder(RootBinder binder, Name scope) {
			this.binder = binder.on(binder.bind().asAuto()).on(
					binder.bind().next()).per(scope);
		}

		private Bindings bindings() {
			return binder.bindings();
		}

		private Mirrors mirrors() {
			return bindings().mirrors;
		}

		public void in(Class<?> service) {
			in(service, Parameter.noParameters);
		}

		public void in(Object service, Parameter<?>... hints) {
			bindMirrorMethodsIn(service.getClass(), service, hints);
		}

		public void in(Class<?> service, Parameter<?>... hints) {
			boolean boundInstanceMethods = bindMirrorMethodsIn(service, null,
					hints);
			if (!boundInstanceMethods)
				return; // do not try to construct the class
			Constructor<?> target = mirrors().construction.reflect(service);
			if (target != null)
				bind(target, hints);
		}

		private boolean bindMirrorMethodsIn(Class<?> impl, Object instance,
				Parameter<?>[] hints) {
			boolean instanceMethods = false;
			for (Method method : mirrors().production.reflect(impl)) {
				Type<?> returns = Type.returnType(method);
				if (returns.rawType != void.class
					&& returns.rawType != Void.class) {
					if (hints.length == 0)
						hints = mirrors().parameterisation.reflect(method);
					binder.bind(mirrors().naming.reflect(method), returns).to(
							instance, method, hints);
					instanceMethods = instanceMethods
						|| !Modifier.isStatic(method.getModifiers());
				}
			}
			return instanceMethods;
		}

		private <T> void bind(Constructor<T> target, Parameter<?>... hints) {
			Name name = mirrors().naming.reflect(target);
			if (hints.length == 0)
				hints = mirrors().parameterisation.reflect(target);
			Class<T> impl = target.getDeclaringClass();
			Binder appBinder = binder.root.per(Scope.application).implicit();
			if (name.isDefault()) {
				appBinder.autobind(impl).to(target, hints);
			} else {
				appBinder.bind(name, impl).to(target, hints);
				for (Type<? super T> st : Type.raw(impl).supertypes())
					if (st.isInterface())
						appBinder.implicit().bind(name, st).to(name, impl);
			}
		}

		public void in(Class<?> impl, Class<?>... more) {
			in(impl);
			for (Class<?> i : more)
				in(i);
		}

		public void inModule() {
			in(binder.bind().source.ident);
		}
	}

	public static class RootBinder extends ScopedBinder {

		RootBinder(Bind bind) {
			super(null, bind);
		}

		public ScopedBinder per(Name scope) {
			return new ScopedBinder(root, bind().per(scope));
		}

		/**
		 * @since 19.1
		 */
		public RootBinder with(Mirrors mirrors) {
			return into(bindings().with(mirrors));
		}

		/**
		 * @since 19.1
		 */
		public RootBinder into(Bindings bindings) {
			return on(bind().into(bindings));
		}

		public RootBinder asDefault() {
			return on(bind().asDefault());
		}

		// OPEN also allow naming for provided instances - this is used for
		// value objects that become parameter; settings required and provided

		public <T> void provide(Class<T> impl, Parameter<?>... hints) {
			on(bind().asProvided()).bind(impl).toConstructor(hints);
		}

		public <T> void require(Class<T> dependency) {
			require(raw(dependency));
		}

		public <T> void require(Type<T> dependency) {
			on(bind().asRequired()).bind(dependency).to(Supply.required(),
					BindingType.REQUIRED);
		}

		@Override
		protected RootBinder on(Bind bind) {
			return new RootBinder(bind);
		}

	}

	public static class ScopedBinder extends TargetedBinder {

		protected ScopedBinder(RootBinder root, Bind bind) {
			super(root, bind);
		}

		/**
		 * Root for container "global" configuration.
		 * 
		 * @since 19.1
		 */
		public TargetedBinder config() {
			return injectingInto(Config.class);
		}

		/**
		 * Root for target type specific configuration.
		 * 
		 * @since 19.1
		 */
		public TargetedBinder config(Class<?> ns) {
			return config().within(ns);
		}

		/**
		 * Root for {@link Instance} specific configuration.
		 * 
		 * @since 19.1
		 */
		public TargetedBinder config(Instance<?> ns) {
			return config().within(ns);
		}

		public TargetedBinder injectingInto(Class<?> target) {
			return injectingInto(raw(target));
		}

		public TargetedBinder injectingInto(Instance<?> target) {
			return new TargetedBinder(root, bind().with(targeting(target)));
		}

		public TargetedBinder injectingInto(Name name, Class<?> type) {
			return injectingInto(name, raw(type));
		}

		public TargetedBinder injectingInto(Name name, Type<?> type) {
			return injectingInto(Instance.instance(name, type));
		}

		public TargetedBinder injectingInto(Type<?> target) {
			return injectingInto(defaultInstanceOf(target));
		}

		/**
		 * @since 19.1
		 */
		public AutoBinder autobind() {
			return new AutoBinder(root, bind().scope);
		}
	}

	public static class TargetedBinder extends Binder {

		protected TargetedBinder(RootBinder root, Bind bind) {
			super(root, bind);
		}

		public Binder in(Packages packages) {
			return with(bind().target.in(packages));
		}

		public Binder inPackageAndSubPackagesOf(Class<?> type) {
			return with(bind().target.inPackageAndSubPackagesOf(type));
		}

		public Binder inPackageOf(Class<?> type) {
			return with(bind().target.inPackageOf(type));
		}

		public Binder inSubPackagesOf(Class<?> type) {
			return with(bind().target.inSubPackagesOf(type));
		}

		public TargetedBinder within(Class<?> parent) {
			return within(raw(parent));
		}

		public TargetedBinder within(Instance<?> parent) {
			return new TargetedBinder(root, bind().within(parent));
		}

		public TargetedBinder within(Name name, Class<?> parent) {
			return within(instance(name, raw(parent)));
		}

		public TargetedBinder within(Name name, Type<?> parent) {
			return within(instance(name, parent));
		}

		public TargetedBinder within(Type<?> parent) {
			return within(anyOf(parent));
		}
	}

	public static class TypedBinder<T> {

		private final Bind bind;
		protected final Resource<T> resource;

		protected TypedBinder(Bind bind, Instance<T> instance) {
			this(bind.next(), new Resource<>(instance, bind.target));
		}

		TypedBinder(Bind bind, Resource<T> resource) {
			this.bind = bind;
			this.resource = resource;
		}

		public <I extends T> void to(Class<I> impl) {
			to(Instance.anyOf(raw(impl)));
		}

		public void to(Constructor<? extends T> target, Parameter<?>... hints) {
			if (hints.length == 0)
				hints = mirrors().parameterisation.reflect(target);
			expand(New.bind(target, hints));
		}

		protected final void to(Object owner, Method target,
				Parameter<?>... hints) {
			if (hints.length == 0)
				hints = mirrors().parameterisation.reflect(target);
			expand(Factory.bind(owner, target, hints));
		}

		protected final void expand(Object value) {
			declareBindingsIn(bind().asType(resource, BindingType.MACRO, null),
					value);
		}

		protected final void expand(BindingType type,
				Supplier<? extends T> supplier) {
			Binding<T> binding = bind().asType(resource, type, supplier);
			declareBindingsIn(binding, binding);
		}

		private void declareBindingsIn(Binding<?> binding, Object value) {
			Bindings bindings = bindings();
			bindings.macros.expandInto(bindings, binding, value);
		}

		public void toSupplier(Supplier<? extends T> supplier) {
			to(supplier, BindingType.PREDEFINED);
		}

		/**
		 * @since 19.1
		 */
		public void toGenerator(Generator<? extends T> generator) {
			toSupplier(new SupplierGeneratorBridge<>(generator));
		}

		/**
		 * @since 19.1
		 */
		public void to(java.util.function.Supplier<? extends T> method) {
			toSupplier((Dependency<? super T> d, Injector i) -> method.get());
		}

		public final void to(T constant) {
			toConstant(constant);
		}

		public final void to(T constant1, T constant2) {
			onMulti().toConstant(constant1).toConstant(constant2);
		}

		public final void to(T constant1, T constant2, T constant3) {
			onMulti().toConstant(constant1).toConstant(constant2).toConstant(
					constant3);
		}

		@SafeVarargs
		public final void to(T constant1, T... constants) {
			TypedBinder<T> multibinder = onMulti().toConstant(constant1);
			for (int i = 0; i < constants.length; i++)
				multibinder.toConstant(constants[i]);
		}

		public void toConstructor() {
			to(mirrors().construction.reflect(resource.type().rawType));
		}

		public void toConstructor(Class<? extends T> impl,
				Parameter<?>... hints) {
			if (isClassVirtual(impl))
				throw InconsistentDeclaration.notConstructible(impl);
			to(mirrors().construction.reflect(impl), hints);
		}

		public void toConstructor(Parameter<?>... hints) {
			toConstructor(getType().rawType, hints);
		}

		public <I extends T> void to(Name name, Class<I> type) {
			to(instance(name, raw(type)));
		}

		public <I extends T> void to(Name name, Type<I> type) {
			to(instance(name, type));
		}

		public <I extends T> void to(Instance<I> instance) {
			expand(instance);
		}

		public <I extends T> void toParametrized(Class<I> impl) {
			expand(impl);
		}

		public <I extends Supplier<? extends T>> void toSupplier(
				Class<I> impl) {
			expand(defaultInstanceOf(raw(impl)));
		}

		protected final void to(Supplier<? extends T> supplier,
				BindingType type) {
			expand(type, supplier);
		}

		private TypedBinder<T> toConstant(T constant) {
			expand(new Constant<>(constant));
			return this;
		}

		final Bind bind() {
			return bind;
		}

		protected final Bindings bindings() {
			return bind().bindings;
		}

		protected final Mirrors mirrors() {
			return bindings().mirrors;
		}

		protected final Type<T> getType() {
			return resource.type();
		}

		protected final TypedBinder<T> on(Bind bind) {
			return new TypedBinder<>(bind, resource);
		}

		protected final TypedBinder<T> onMulti() {
			return on(bind().asMulti());
		}

	}

	/**
	 * This kind of bindings actually re-map the []-type so that the automatic
	 * behavior of returning all known instances of the element type will no
	 * longer be used whenever the bind made applies.
	 *
	 * @author Jan Bernitt (jan@jbee.se)
	 *
	 */
	public static class TypedElementBinder<E> extends TypedBinder<E[]> {

		protected TypedElementBinder(Bind bind, Instance<E[]> instance) {
			super(bind.asMulti().next(), instance);
		}

		public void toElements(Parameter<? extends E> elem1) {
			expandElements(elem1);
		}

		public void toElements(Parameter<? extends E> elem1,
				Parameter<? extends E> elem2) {
			expandElements(elem1, elem2);
		}

		public void toElements(Parameter<? extends E> elem1,
				Parameter<? extends E> elem2, Parameter<? extends E> elem3) {
			expandElements(elem1, elem2, elem3);
		}

		@SafeVarargs
		public final void toElements(Parameter<? extends E>... elems) {
			expand(elems);
		}

		@SafeVarargs
		private final void expandElements(Parameter<? extends E>... hints) {
			expand(hints);
		}

		public void toElements(E c1) {
			to(array(c1));
		}

		public void toElements(E c1, E c2) {
			to(array(c1, c2));
		}

		public void toElements(E c1, E c2, E c3) {
			to(array(c1, c2, c3));
		}

		@SafeVarargs
		public final void toElements(E... constants) {
			to(array(constants));
		}

		@SuppressWarnings("unchecked")
		private E[] array(Object... elements) {
			Class<E[]> rawType = getType().rawType;
			if (elements.getClass() == rawType)
				return (E[]) elements;
			Object[] res = newArray(getType().baseType().rawType,
					elements.length);
			System.arraycopy(elements, 0, res, 0, res.length);
			return (E[]) res;
		}
	}

	/**
	 * This cannot be changed to a lambda since we need a type that actually
	 * implements both {@link Supplier} and {@link Generator}. This way the
	 * {@link Generator} is picked directly by the {@link Injector}.
	 */
	private static final class SupplierGeneratorBridge<T>
			implements Supplier<T>, Generator<T> {

		private final Generator<T> generator;

		SupplierGeneratorBridge(Generator<T> generator) {
			this.generator = generator;
		}

		@Override
		public T yield(Dependency<? super T> dep)
				throws UnresolvableDependency {
			return generator.yield(dep);
		}

		@Override
		public T supply(Dependency<? super T> dep, Injector context)
				throws UnresolvableDependency {
			return yield(dep);
		}

	}
}