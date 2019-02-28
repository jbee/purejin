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
import static se.jbee.inject.Name.pluginFor;
import static se.jbee.inject.Source.source;
import static se.jbee.inject.Target.targeting;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.bootstrap.Metaclass.metaclass;
import static se.jbee.inject.container.Typecast.initialiserTypeOf;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.BiConsumer;

import se.jbee.inject.Array;
import se.jbee.inject.Dependency;
import se.jbee.inject.InconsistentBinding;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Packages;
import se.jbee.inject.Parameter;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Target;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Binding;
import se.jbee.inject.bootstrap.BindingType;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.BoundConstant;
import se.jbee.inject.bootstrap.BoundConstructor;
import se.jbee.inject.bootstrap.BoundMethod;
import se.jbee.inject.bootstrap.Supply;
import se.jbee.inject.config.ConstructionMirror;
import se.jbee.inject.config.NamingMirror;
import se.jbee.inject.config.ParameterisationMirror;
import se.jbee.inject.config.ProductionMirror;
import se.jbee.inject.container.Factory;
import se.jbee.inject.container.Initialiser;
import se.jbee.inject.container.Scoped;
import se.jbee.inject.container.Supplier;

/**
 * The default implementation of a fluent binder interface that provides a lot
 * of utility methods to improve readability and keep binding compact.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
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
			binder.bind(pluginFor(pluginPoint, property), Class.class).to(
					plugin);
			if (!metaclass(plugin).undeterminable()) {
				binder.implicit().construct(plugin);
			}
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

		protected AutoBinder(RootBinder binder, Scope scope) {
			this.binder = binder.on(binder.bind().asAuto()).on(
					binder.bind().next()).per(scope);
		}

		private Bindings bindings() {
			return binder.bind().bindings;
		}

		public void in(Class<?> service) {
			in(service, new Parameter<?>[0]);
		}

		public void in(Object service, Parameter<?>... params) {
			bindMethodsIn(service.getClass(), service, params);
		}

		public void in(Class<?> service, Parameter<?>... params) {
			boolean boundInstanceMethods = bindMethodsIn(service, null, params);
			if (!boundInstanceMethods)
				return; // do not try to construct the class
			Constructor<?> c = bindings().construction.reflect(service);
			if (c != null) {
				bind(c, params);
			}
		}

		private boolean bindMethodsIn(Class<?> implementer, Object instance,
				Parameter<?>[] params) {
			boolean instanceMethods = false;
			Bindings bindings = bindings();
			for (Method method : bindings.production.reflect(implementer)) {
				Type<?> returnType = Type.returnType(method);
				//TODO why do this check below???
				if (!Type.VOID.equalTo(returnType)) {
					if (params.length == 0) {
						params = bindings.parameterisation.reflect(method);
					}
					binder.bind(bindings.naming.reflect(method), returnType).to(
							instance, method, params);
					instanceMethods = instanceMethods
						|| !Modifier.isStatic(method.getModifiers());
				}
			}
			return instanceMethods;
		}

		private <T> void bind(Constructor<T> c, Parameter<?>... params) {
			Name name = bindings().naming.reflect(c);
			if (params.length == 0)
				params = bindings().parameterisation.reflect(c);
			Class<T> impl = c.getDeclaringClass();
			Binder appBinder = binder.root.per(Scoped.APPLICATION).implicit();
			if (name.isDefault()) {
				appBinder.autobind(impl).to(c, params);
			} else {
				appBinder.bind(name, impl).to(c, params);
				for (Type<? super T> st : Type.raw(impl).supertypes()) {
					if (st.isInterface()) {
						appBinder.implicit().bind(name, st).to(name, impl);
					}
				}
			}
		}

		public void in(Class<?> impl, Class<?>... more) {
			in(impl);
			for (Class<?> i : more) {
				in(i);
			}
		}

		public void inModule() {
			in(binder.bind().source.ident);
		}
	}

	public static class RootBinder extends ScopedBinder {

		RootBinder(Bind bind) {
			super(null, bind);
		}

		public ScopedBinder per(Scope scope) {
			return new ScopedBinder(root, bind().per(scope));
		}

		/**
		 * @since 19.1
		 */
		public RootBinder constructs(ConstructionMirror reflector) {
			return on(bind().into(bind().bindings.with(reflector)));
		}

		/**
		 * @since 19.1
		 */
		public RootBinder produces(ProductionMirror reflector) {
			return on(bind().into(bind().bindings.with(reflector)));
		}

		/**
		 * @since 19.1
		 */
		public RootBinder parameterises(ParameterisationMirror reflector) {
			return on(bind().into(bind().bindings.with(reflector)));
		}

		/**
		 * @since 19.1
		 */
		public RootBinder names(NamingMirror reflector) {
			return on(bind().into(bind().bindings.with(reflector)));
		}

		public RootBinder asDefault() {
			return on(bind().asDefault());
		}

		// OPEN also allow naming for provided instances - this is used for
		// value objects that become parameter

		public <T> void provide(Class<T> implementation,
				Parameter<?>... parameters) {
			on(bind().asProvided()).bind(implementation).toConstructor(
					parameters);
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

		public void to(Constructor<? extends T> constructor,
				Parameter<?>... parameters) {
			expand(BoundConstructor.bind(constructor, parameters));
		}

		protected final void to(Object instance, Method method,
				Parameter<?>[] parameters) {
			expand(BoundMethod.bind(instance, method, Type.returnType(method),
					parameters));
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
			Bindings bindings = bind().bindings;
			bindings.macros.expandInto(bindings, binding, value);
		}

		public void to(Factory<? extends T> factory) {
			to(Supply.factory(factory));
		}

		public void to(Supplier<? extends T> supplier) {
			to(supplier, BindingType.PREDEFINED);
		}

		public void to(java.util.function.Supplier<? extends T> method) {
			to((Supplier<? extends T>) (Dependency<? super T> d,
					Injector i) -> method.get());
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
			for (int i = 0; i < constants.length; i++) {
				multibinder.toConstant(constants[i]);
			}
		}

		public void toConstructor() {
			to(bind().bindings.construction.reflect(resource.type().rawType));
		}

		public void toConstructor(Class<? extends T> impl,
				Parameter<?>... params) {
			if (metaclass(impl).undeterminable()) {
				throw new InconsistentBinding(
						"Not a constructable type: " + impl);
			}
			to(bind().bindings.construction.reflect(impl), params);
		}

		public void toConstructor(Parameter<?>... params) {
			toConstructor(getType().rawType, params);
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
			expand(new BoundConstant<>(constant));
			return this;
		}

		final Bind bind() {
			return bind;
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

		@SuppressWarnings("unchecked")
		public void toElements(Parameter<? extends E> p1) {
			toElements(new Parameter[] { p1 });
		}

		@SuppressWarnings("unchecked")
		public void toElements(Parameter<? extends E> p1,
				Parameter<? extends E> p2) {
			toElements(new Parameter[] { p1, p2 });
		}

		@SuppressWarnings("unchecked")
		public void toElements(Parameter<? extends E> p1,
				Parameter<? extends E> p2, Parameter<? extends E> p3) {
			toElements(new Parameter[] { p1, p2, p3 });
		}

		@SafeVarargs
		public final void toElements(Parameter<? extends E>... parameters) {
			expand(parameters);
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
			if (elements.getClass() == rawType) {
				return (E[]) elements;
			}
			Object[] a = Array.newInstance(getType().baseType().rawType,
					elements.length);
			System.arraycopy(elements, 0, a, 0, a.length);
			return (E[]) a;
		}
	}

}