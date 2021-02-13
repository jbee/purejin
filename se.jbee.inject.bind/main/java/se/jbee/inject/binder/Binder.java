/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.*;
import se.jbee.inject.bind.*;
import se.jbee.inject.binder.spi.*;
import se.jbee.inject.config.*;
import se.jbee.lang.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Hint.relativeReferenceTo;
import static se.jbee.inject.Instance.*;
import static se.jbee.inject.Name.DEFAULT;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Source.source;
import static se.jbee.inject.Target.targeting;
import static se.jbee.inject.binder.Constructs.constructs;
import static se.jbee.inject.binder.spi.ConnectorBinder.CONNECT_QUALIFIER;
import static se.jbee.inject.config.Plugins.pluginPoint;
import static se.jbee.inject.config.ProducesBy.declaredMethods;
import static se.jbee.lang.Type.raw;
import static se.jbee.lang.Utils.isClassConstructable;
import static se.jbee.lang.Utils.newArray;

/**
 * The default implementation of a fluent binder API that provides a lot
 * of utility methods to improve readability and keep binding compact.
 */
@SuppressWarnings({ "squid:S1448", "squid:S1200", "ClassReferencesSubclass" })
public class Binder {

	public static RootBinder create(Bind bind) {
		return new RootBinder(bind);
	}

	protected final RootBinder root;
	private final Bind bind;

	Binder(RootBinder root, Bind bind) {
		this.root = root == null ? (RootBinder) this : root;
		this.bind = bind.source == null ? bind.with(source(getClass())) : bind;
	}

	public Bind bind() {
		return bind; // !OBS! This method might be overridden to update
					// Bind properties - do not access the field directly
	}

	protected final Env env() {
		return bind().env;
	}

	protected final Bindings bindings() {
		return bind().bindings;
	}

	/**
	 * Adds bindings indirectly by inspecting the annotations present on the
	 * provided types and their methods. An annotation can be linked to a
	 * binding pattern described by a {@link ModuleWith} linked to a specific
	 * annotation as part of the {@link Env}. This can done either
	 * programmatically or via {@link java.util.ServiceLoader} entry for the
	 * {@link ModuleWith} (of {@link Class}).
	 * <p>
	 * This is the most "automatic" way of binding that maybe has closes
	 * similarity with annotation based dependency injection as found in CDI or
	 * spring. It separates the specifics of the bind from the target. While
	 * this is very simple to use it is also very limiting.
	 *
	 * @param types all types to bind using annotations present on the type and
	 *              their methods
	 */
	public void detectAt(Class<?>... types) {
		Bindings bindings = bindings();
		for (Class<?> type : types) {
			bindings.addAnnotated(bind().env, type);
			implicit().bind(type).toConstructor();
		}
	}

	/**
	 * All bindings made with the returned {@link Binder} will only allow access
	 * (injection/resolving) when it is done via an interface type.
	 *
	 * @return immutable fluent API
	 */
	public Binder withIndirectAccess() {
		return with(bind().target.indirect());
	}

	/**
	 * Binds all implemented types that are considered an API by
	 * the {@link PublishesBy} strategy defined in the {@link Env}.
	 * <p>
	 * For example binding {@link Integer} with {@link PublishesBy#SUPER} (all
	 * super classes) adds references that bind {@link Number} to {@link
	 * Integer}, {@link java.io.Serializable} to {@link Integer} and so forth
	 * for all types it does implement.
	 * <p>
	 * Should these reference clash with another explicit binding, for example
	 * {@link Number} was bound to some other value provider, the explicit
	 * binding takes precedence. Also several contract-bound bindings from the
	 * same type to different implementors do not clash and are removed because
	 * they are ambiguous. So usually using {@code withContractAccess} does not
	 * create issues with clashing bindings.
	 * <p>
	 * Note also that the usage of this "modifier" allows to declare
	 * combinations that do not make much sense. For example using a {@code
	 * to}-clause that refers to a sub-type.
	 *
	 * @return a {@link Binder} that binds all subsequent {@code bind} calls not
	 * as the exact type but as all published APIs types.
	 */
	public final Binder withPublishedAccess() {
		return on(bind().asPublished());
	}

	/**
	 * Binds all implemented types that are considered an API by the provided
	 * {@link PublishesBy} strategy.
	 * <p>
	 * Note that this means APIs added to the environment are not considered.
	 * Only the provided {@link PublishesBy} strategy is considered.
	 *
	 * @param strategy The predicate to find out what supertypes to consider
	 *                 APIs of a given implementation type and which around
	 *                 bound to the implementation as a consequence of it.
	 * @return a {@link Binder} that binds all subsequent {@code bind} calls not
	 * as the exact type but as all published APIs types.
	 */
	public final Binder withPublishedAccess(PublishesBy strategy) {
		return on(bind().asPublished().with(env().with(PublishesBy.class, strategy)));
	}

	/**
	 * Explicitly binds an array type to a specific list of elements.
	 *
	 * Note that this method is only used in case an array type should be bound explicitly.
	 * To make several independent bindings that can be injected as array, set or list
	 * use {@link #multibind(Name, Type)}s.
	 *
	 * @see #multibind(Name, Type)
	 *
	 * @param type the array type that is used elsewhere (the API or "interface")
	 * @param <E> the type of the bound array elements
	 * @return immutable fluent API for array element bindings
	 */
	public <E> TypedElementBinder<E> arraybind(Class<E[]> type) {
		return new TypedElementBinder<>(bind(), defaultInstanceOf(raw(type)));
	}

	/**
	 * Bind an instance with default {@link Name} (unnamed instance).
	 *
	 * @param type the type that is used elsewhere (the API or interface)
	 * @param <T> raw type reference to the bound type
	 * @return immutable fluent API
	 */
	public final <T> TypedBinder<T> bind(Class<T> type) {
		return bind(raw(type));
	}

	/**
	 * Bind an instance with default {@link Name} (unnamed instance).
	 *
	 * @param type the type that is used elsewhere (the API or interface)
	 * @param <T>  type reference to the bound fully generic {@link Type}
	 * @return immutable fluent API
	 */
	public final <T> TypedBinder<T> bind(Type<T> type) {
		return bind(defaultInstanceOf(type));
	}

	/**
	 * Same as {@link #bind(Name, Type)} just that both arguments are provided
	 * in form of an {@link Instance}.
	 *
	 * @see #bind(Name, Type)
	 *
	 * @param instance the name and type the bound instance should be known as
	 * @param <T>      type reference to the bound fully generic {@link Type}
	 * @return immutable fluent API
	 */
	public <T> TypedBinder<T> bind(Instance<T> instance) {
		return new TypedBinder<>(bind(), instance);
	}

	public final <T> TypedBinder<T> bind(String name, Class<T> type) {
		return bind(named(name), type);
	}

	public final <T> TypedBinder<T> bind(Name name, Class<T> type) {
		return bind(name, raw(type));
	}

	public final <T> TypedBinder<T> bind(Name name, Type<T> type) {
		return bind(instance(name, type));
	}

	/**
	 * Construct an instance of the provided type with default name (unnamed).
	 *
	 * Just a short from of {@code bind(type).toConstructor()}.
	 *
	 * @param type both the implementation type and the type the created
	 *             instance(s) should be known as
	 */
	public final void construct(Class<?> type) {
		construct((defaultInstanceOf(raw(type))));
	}

	/**
	 * Construct a named instance of the provided type.
	 *
	 * Just a short from of {@code bind(instance).toConstructor()}.
	 *
	 * @param instance both the implementation type and the name and type the
	 *                 created instance(s) should be known as
	 */
	public final void construct(Instance<?> instance) {
		bind(instance).toConstructor();
	}

	/**
	 * Construct a named instance of the provided type.
	 *
	 * Just a short from of {@code bind(name, type).toConstructor()}.
	 *
	 * @param name the name the created instance(s) should be known as
	 * @param type both the implementation type and the type the created
	 *             instance(s) should be known as
	 */
	public final void construct(String name, Class<?> type) {
		construct(instance(named(name), raw(type)));
	}

	/**
	 * Bind a {@link Lift} for the {@link Injector} itself.
	 *
	 * @return immutable fluent API
	 */
	public final LiftBinder<Injector> lift() {
		return lift(Injector.class);
	}

	/**
	 * Bind a {@link Lift} that affects all types assignable to provided type.
	 *
	 * @return immutable fluent API
	 */
	public final <T> LiftBinder<T> lift(Class<T> type) {
		return lift(raw(type));
	}

	/**
	 * Bind a {@link Lift} that affects all types assignable to provided type.
	 *
	 * @return immutable fluent API
	 */
	public final <T> LiftBinder<T> lift(Type<T> type) {
		return multibind(Lift.liftTypeOf(type)).wrapAs(LiftBinder::new);
	}

	public final <T> TypedBinder<T> multibind(Class<T> type) {
		return multibind(raw(type));
	}

	public final <T> TypedBinder<T> multibind(Instance<T> instance) {
		return on(bind().asMulti()).bind(instance);
	}

	public final <T> TypedBinder<T> multibind(String name, Class<T> type) {
		return multibind(instance(named(name), raw(type)));
	}

	public final <T> TypedBinder<T> multibind(Name name, Type<T> type) {
		return multibind(instance(name, type));
	}

	public final <T> TypedBinder<T> multibind(Type<T> type) {
		return multibind(defaultInstanceOf(type));
	}

	public final <T> TypedBinder<T> starbind(Class<T> type) {
		return bind(anyOf(raw(type)));
	}

	public <T> PluginBinder<T> plug(Class<T> plugin) {
		return new PluginBinder<>(on(bind().next()), plugin);
	}

	/**
	 * Mark methods as members of a named group.
	 *
	 * @since 8.1
	 */
	public final ConnectBinder connect() {
		Env env = env();
		return connect(env.property(CONNECT_QUALIFIER, ProducesBy.class,
						env.property(ProducesBy.class)));
	}

	public ConnectBinder connect(ProducesBy connectsBy) {
		return new ConnectBinder(this, connectsBy);
	}

	public <T> ConnectTargetBinder<T> connect(Class<T> api) {
		return new ConnectTargetBinder<>(this,
				ProducesBy.OPTIMISTIC.in(api), raw(api));
	}

	public void scheduleIn(Class<?> bean, Class<? extends Annotation> schedule) {
		connect(declaredMethods(true).annotatedWith(schedule)) //
			.inAny(bean) //
			.asScheduled(named(schedule));
	}

	public void receiveIn(Class<?> bean, Class<? extends Annotation> onEvent) {
		connect(declaredMethods(true).annotatedWith(onEvent)) //
			.inAny(bean) //
			.asEvent();
	}

	protected Binder on(Bind bind) {
		return new Binder(root, bind);
	}

	protected final Binder implicit() {
		return on(bind().asImplicit());
	}

	protected final Binder with(Target target) {
		return new Binder(root, bind().with(target));
	}

	/**
	 * @see #installIn(String, Class...)
	 * @since 8.1
	 */
	@SafeVarargs
	public final void installIn(Class<?> subContext,
			Class<? extends Bundle>... lazyInstalled) {
		installIn(subContext.getName(), lazyInstalled);
	}

	/**
	 * Binds a lazy {@link Bundle} which is installed in a sub-context
	 * {@link Injector}. The {@link Injector} is bootstrapped lazily on first
	 * usage. Use {@link Injector#subContext(String)} to resolve the sub-context
	 * by name.
	 *
	 * @param subContext the name of the lazy {@link Injector} sub-context
	 * @param lazyInstalled the {@link Bundle} to install in the sub-context
	 *            lazy {@link Injector}
	 *
	 * @since 8.1
	 */
	@SafeVarargs
	public final void installIn(String subContext,
			Class<? extends Bundle>... lazyInstalled) {
		if (lazyInstalled.length > 0) {
			for (Class<? extends Bundle> bundle : lazyInstalled)
				plug(bundle).into(Injector.class, subContext);
		}
	}

	/**
	 *
	 * @param target the type whose instances should be initialised by calling
	 *            some method
	 * @since 8.1
	 */
	public final <T> BootBinder<T> boot(Class<T> target) {
		return boot(Name.DEFAULT, raw(target));
	}

	public <T> BootBinder<T> boot(Name name, Type<T> target) {
		return new BootBinder<>(on(bind().next()), instance(name, target));
	}

	/**
	 * Small utility to <b>lazily</b> run an "initialisation" function for the
	 * target instances of the bound {@link Lift}.
	 *
	 * @param <T> type of the instances being {@link Lift}
	 * @since 8.1
	 */
	public static class LiftBinder<T> extends TypedBinder<Lift<T>> {

		LiftBinder(Bind bind, Locator<Lift<T>> locator) {
			super(bind, locator);
		}

		public final void run(Consumer<T> function) {
			to((target, as, context) -> {
				function.accept(target);
				return target;
			});
		}

		public final void run(UnaryOperator<T> function) {
			to(((target, as, context) -> function.apply(target)));
		}

		public <R> void by(Instance<? extends R> related,
				BiConsumer<T, R> initFunction) {
			to((target, as, context) -> {
				initFunction.accept(target,
						context.resolve(dependency(related).injectingInto(as)));
				return target;
			});
		}

		public final <R> void forEach(Class<? extends R> related,
				BiConsumer<T, R> initFunction) {
			forEach(raw(related), initFunction);
		}

		public <R> void forEach(Type<? extends R> related,
				BiConsumer<T, R> initFunction) {
			Dependency<? extends R[]> dep = dependency(
					related.addArrayDimension().asUpperBound());
			to((target, as, context) -> {
				for (R arg : context.resolve(dep.injectingInto(as)))
					initFunction.accept(target, arg);
				return target;
			});
		}
	}

	/**
	 * Small utility to <b>eagerly</b> run an initialisation function for a
	 * group instances managed by the {@link Injector} at the end of the
	 * bootstrapping process.
	 * <p>
	 * The basic principle is that the {@link #target} {@link Instance} is
	 * initialised on the basis of some other dependency instance that is
	 * resolved during initialisation phase and provided to the {@link
	 * BiConsumer} function. This can be one or many of such instances.
	 * <p>
	 * In contrast to a plain {@link Lift} that runs <b>lazily</b> when an
	 * instance of the matching type is constructed this initialisation is
	 * performed directly after the {@link Injector} context is created.
	 *
	 * @param <T> type of the instances that should be setup during
	 *            bootstrapping
	 * @since 8.1
	 */
	public static class BootBinder<T> {

		private final Binder binder;
		private final Instance<T> target;

		protected BootBinder(Binder binder, Instance<T> target) {
			this.binder = binder;
			this.target = target;
		}

		public final <R> void forEach(Class<? extends R> related,
				BiConsumer<T, R> initFunction) {
			forEach(raw(related), initFunction);
		}

		public <R> void forEach(Type<? extends R> related,
				BiConsumer<T, R> initFunction) {
			Dependency<? extends R[]> dep = dependency(
					related.addArrayDimension().asUpperBound()) //
					.injectingInto(target);
			binder.lift().to((impl, as, context) -> {
				T obj = context.resolve(target);
				for (R arg : context.resolve(dep))
					initFunction.accept(obj, arg);
				return impl;
			});
		}

		public final <R> void by(Class<? extends R> related,
				BiConsumer<T, R> initFunction) {
			by(defaultInstanceOf(raw(related)), initFunction);
		}

		public final <R> void by(Name relatedName,
				Class<? extends R> relatedType, BiConsumer<T, R> initFunction) {
			by(Instance.instance(relatedName, raw(relatedType)), initFunction);
		}

		public <R> void by(Instance<? extends R> related,
				BiConsumer<T, R> initFunction) {
			binder.lift().to((impl, as, context) -> {
				T obj = context.resolve(target);
				R arg = context.resolve(
						dependency(related).injectingInto(as));
				initFunction.accept(obj, arg);
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

		public final void into(Class<?> pluginPoint) {
			into(pluginPoint, plugin.getCanonicalName());
		}

		public void into(Class<?> pluginPoint, String property) {
			binder.multibind(pluginPoint(pluginPoint, property),
					raw(Class.class)).to(plugin);
			if (isClassConstructable(plugin))
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
	 * Connecting is the dynamic process of identifying methods in target types
	 * that should be subject to a {@link Connector} referenced by name.
	 *
	 * The {@link Connector} is expected to be bound explicitly elsewhere.
	 *
	 * @since 8.1
	 */
	public static class ConnectBinder {

		private final Binder binder;
		private final ProducesBy connectsBy;

		protected ConnectBinder(Binder binder, ProducesBy connectsBy) {
			this.binder = binder;
			this.connectsBy = connectsBy;
		}

		/**
		 * @see #inAny(Type)
		 */
		public final <T> ConnectTargetBinder<T> inAny(Class<T> target) {
			return inAny(raw(target));
		}

		/**
		 * Connecting is applied to all subtypes of the provided target type.
		 *
		 * @param target can be understood as the scope in which connecting applies
		 *               to identified methods.
		 * @param <T>    target bean type or interface implemented by targets
		 * @return binder for fluent API
		 */
		public <T> ConnectTargetBinder<T> inAny(Type<T> target) {
			return new ConnectTargetBinder<>(binder, connectsBy, target);
		}
	}

	/**
	 * @param <T> type of the class(es) (includes subtypes) that are subject to
	 *            connecting
	 * @since 8.1
	 */
	public static final class ConnectTargetBinder<T> implements ConnectorBinder {

		private final Binder binder;
		private final ProducesBy connectsBy;
		private final Type<T> target;

		public ConnectTargetBinder(Binder binder, ProducesBy connectsBy,
				Type<T> target) {
			this.binder = binder;
			this.connectsBy = connectsBy;
			this.target = target;
		}

		@Override
		public void to(Name connectorName) {
			binder.lift(target).to((instance, as, context) -> //
					connect(connectorName, instance, as, context));
		}

		private T connect(Name connectorName, T instance, Type<?> as,
				Injector context) {
			Method[] connected = connectsBy.reflect(instance.getClass());
			if (connected != null && connected.length > 0) {
				Connector connector = context.resolve(connectorName, Connector.class);
				for (Method m : connected)
					connector.connect(instance, as, m);
			}
			return instance;
		}
	}

	/**
	 * The {@link AutoBinder} makes use of mirrors to select and bind
	 * constructors for beans and methods as factories and {@link Name} these
	 * instances as well as provide {@link Hint}s.
	 *
	 * @since 8.1
	 */
	public static class AutoBinder {

		private final RootBinder binder;
		private final Env env;

		protected AutoBinder(RootBinder binder, Name scope) {
			this(binder.on(binder.bind().asPublished())
					.on(binder.bind().next()), env(binder, scope));
		}

		private static Env env(RootBinder binder, Name scope) {
			Env env = binder.env().withIsolate();
			return scope.equalTo(Scope.mirror)
					? env
					: env.with(ScopesBy.class, target -> scope);
		}

		private AutoBinder(RootBinder binder, Env env) {
			this.binder = binder;
			this.env = env;
		}

		private NamesBy namesBy() {
			return env.property(NamesBy.class).orElse(DEFAULT);
		}

		private HintsBy hintsBy() {
			return env.property(HintsBy.class);
		}

		private ScopesBy scopesBy() {
			return env.property(ScopesBy.class);
		}

		private AutoBinder with(Env env) {
			return new AutoBinder(binder, env);
		}

		public final AutoBinder accessBy(AccessesBy strategy) {
			return with(env.with(AccessesBy.class, strategy));
		}

		public final AutoBinder constructBy(ConstructsBy strategy) {
			return with(env.with(ConstructsBy.class, strategy));
		}

		public final AutoBinder produceBy(ProducesBy strategy) {
			return with(env.with(ProducesBy.class, strategy));
		}

		public final AutoBinder nameBy(NamesBy strategy) {
			return with(env.with(NamesBy.class, strategy));
		}

		public final AutoBinder scopeBy(ScopesBy strategy) {
			return with(env.with(ScopesBy.class, strategy));
		}

		public final AutoBinder hintBy(HintsBy strategy) {
			return with(env.with(HintsBy.class, strategy));
		}

		public final AutoBinder publishBy(PublishesBy strategy) {
			return with(env.with(PublishesBy.class, strategy));
		}

		public final void in(Class<?> impl) {
			in(impl, Hint.none());
		}

		public final void in(Class<?> impl, Class<?>... more) {
			in(impl);
			for (Class<?> i : more)
				in(i);
		}

		public final void in(Object impl, Hint<?>... construction) {
			if (impl instanceof Hint) {
				in(((Hint<?>) impl).asType.rawType, impl, construction);
			} else {
				in(impl.getClass(), impl, construction);
			}
		}

		public final void in(Class<?> impl, Hint<?>... construction) {
			in(impl, null, construction);
		}

		private void in(Class<?> impl, Object instance,
				Hint<?>... construction) {
			boolean needsInstance1 = bindProducesIn(impl, instance);
			boolean needsInstance2 = bindAccessesIn(impl, instance);
			if (!needsInstance1 && !needsInstance2)
				return; // do not try to construct the class
			if (instance != null && !(instance instanceof Hint))
				return; // if there is an instance don't bind constructor unless it is just a Hint
			Constructor<?> target = env.property(ConstructsBy.class)
					.reflect(impl.getDeclaredConstructors(), construction);
			if (target != null)
				toConstructor(Scope.auto, target, construction);
		}

		private boolean bindAccessesIn(Class<?> impl, Object instance) {
			Field[] constants = env.property(AccessesBy.class).reflect(impl);
			if (constants == null || constants.length == 0)
				return false;
			boolean needsInstance = false;
			for (Field constant : constants) {
				if (asConstant(constant, instance))
					needsInstance |= !isStatic(constant.getModifiers());
			}
			return needsInstance;
		}

		private boolean bindProducesIn(Class<?> impl, Object instance) {
			Method[] producers = env.property(ProducesBy.class).reflect(impl);
			if (producers == null)
				return false;
			boolean needsInstance = false;
			for (Method producer : producers) {
				if (asProducer(producer, instance, Hint.none()))
					needsInstance |= !isStatic(producer.getModifiers());
			}
			return needsInstance;
		}

		public boolean asConstant(Field constant, Object instance) {
			Accesses<?> accesses = Accesses.accesses(instance, constant);
			binder.per(Scope.container) //
					.bind(namesBy().reflect(constant), accesses.expectedType) //
					.expand(accesses);
			return true;
		}

		/**
		 * This method will not make sure an instance of the {@link Method}'s
		 * declaring class is created if needed. This must be bound elsewhere.
		 *
		 * @param target   a method that is meant to create instances of the
		 *                 method return type
		 * @param instance can be null to resolve the instance from {@link
		 *                 Injector} context later (if needed). It can also be a
		 *                 {@link Hint} to that instance. {@code null} for
		 *                 {@code static} methods.
		 * @param args     optional method argument {@link Hint}s
		 * @return true if a target was bound, else false (this is e.g. the case
		 * when the {@link Method} returns void)
		 */
		public boolean asProducer(Method target, Object instance, Hint<?>... args) {
			if (target.getReturnType() == void.class || target.getReturnType() == Void.class)
				return false;
			Name scope = scopesBy().reflect(target);
			Produces<?> produces = Produces.produces(instance, target, hintsBy(), args);
			binder.per(scope == null ? Scope.auto : scope) //
					.bind(namesBy().reflect(target), produces.expectedType) //
					.expand(produces);
			return true;
		}

		public final <T> void toConstructor(Constructor<T> target, Hint<?>... hints) {
			toConstructor(scopesBy().reflect(target), target, hints);
		}

		public <T> void toConstructor(Name scope, Constructor<T> target, Hint<?>... hints) {
			if (target == null)
				throw InconsistentBinding.generic("Provided Constructor was null");
			Name name = namesBy().reflect(target);
			Class<T> impl = target.getDeclaringClass();
			Binder scopedBinder = binder.per(scope != null ? scope : Scope.auto).implicit();
			scopedBinder.bind(name, impl).toConstructor(target, hints);
			if (name.isDefault()) {
				scopedBinder.withPublishedAccess(env.property(PublishesBy.class)).bind(impl)
						//TODO use actual type hint/ref
						.expand(constructs(raw(impl), target, env, hints));
			} else {
				for (Type<? super T> st : raw(impl).supertypes())
					if (st.isInterface())
						scopedBinder.implicit().bind(name, st).to(name, impl);
			}
		}
	}

	public static class RootBinder extends ScopedBinder {

		public RootBinder(Bind bind) {
			super(null, bind);
		}

		public ScopedBinder per(Name scope) {
			return new ScopedBinder(root, bind().per(scope));
		}

		public RootBinder asDefault() {
			return on(bind().asDefault());
		}

		// OPEN also allow naming for provided instances - this is used for
		// value objects that become parameter; settings required and provided

		public final <T> void provide(Class<T> impl, Hint<?>... hints) {
			on(bind().asProvided()).bind(impl).toConstructor(hints);
		}

		public final <T> void require(Class<T> apiImplementation) {
			require(raw(apiImplementation));
		}

		public <T> void require(Type<T> apiImplementation) {
			on(bind().asRequired()).bind(apiImplementation) //
					.to(Supply.required(), BindingType.REQUIRED);
		}

		@Override
		protected RootBinder on(Bind bind) {
			return new RootBinder(bind);
		}

	}

	public static class ScopedBinder extends TargetedBinder implements
			ConfiguringInstanceLocalBinder<TargetedBinder> {

		protected ScopedBinder(RootBinder root, Bind bind) {
			super(root, bind);
		}

		@Override
		public TargetedBinder injectingInto(Instance<?> target) {
			return new TargetedBinder(root, bind().with(targeting(target)));
		}

		/**
		 * Bind {@link Method}s and {@link Constructor}s based on strategies.
		 *
		 * @since 8.1
		 */
		public AutoBinder autobind() {
			return new AutoBinder(root, bind().scope);
		}
	}

	public static class TargetedBinder extends Binder
			implements ParentLocalBinder<TargetedBinder>,
			PackageLocalBinder<Binder> {

		protected TargetedBinder(RootBinder root, Bind bind) {
			super(root, bind);
		}

		public final Binder locally() {
			return inPackageOf(bind().source.ident);
		}

		public final Binder in(Packages packages) {
			return with(bind().target.in(packages));
		}

		public TargetedBinder within(Instance<?> parent) {
			return new TargetedBinder(root, bind().within(parent));
		}
	}

	public static class TypedBinder<T> implements ReferenceBinder<T>,
			SupplierBinder<T> {

		private final Bind bind;
		protected final Locator<T> locator;

		protected TypedBinder(Bind bind, Instance<T> instance) {
			this(bind.next(), new Locator<>(instance, bind.target));
		}

		protected TypedBinder(Bind bind, Locator<T> locator) {
			this.bind = bind;
			this.locator = locator;
		}

		protected <B> B wrapAs(BiFunction<Bind, Locator<T>, B> wrap) {
			return wrap.apply(bind, locator);
		}

		private Env env() {
			return bind().env;
		}

		private <P> P env(Class<P> property) {
			return env().property(property);
		}

		public void toConstructor(Constructor<? extends T> target, Hint<?>... args) {
			if (target == null)
				throw InconsistentBinding.generic("Provided constructor was null");
			expand(constructs(locator.type(), target, env(), args));
		}

		protected final void expand(Descriptor value) {
			declareBindingsIn(bind() //
					.asType(locator, BindingType.VALUE, null), value);
		}

		protected final void expand(BindingType type,
				Supplier<? extends T> supplier) {
			Binding<T> binding = bind().asType(locator, type, supplier);
			declareBindingsIn(binding, binding);
		}

		private void declareBindingsIn(Binding<?> binding, Descriptor value) {
			bindings().addExpanded(env(), binding, value);
		}

		@Override
		public final void toSupplier(Supplier<? extends T> supplier) {
			to(supplier, BindingType.PREDEFINED);
		}

		/**
		 * By default constants are not scoped. This implies that no
		 * initialisation occurs for constants as they are directly returned
		 * from the {@link Generator} which simply holds the returned constant
		 * value.
		 * <p>
		 * In contrast to {@link #to(Object)} a scoped constant exist within a
		 * {@link Scope} like instances created by the container. This has the
		 * "side-effect" that {@link Lift} initialisation as usual.
		 *
		 * @param constant a "bean" instance
		 * @since 8.1
		 */
		public final void toScoped(T constant) {
			expand(new Constant<>(constant).scoped());
		}

		public final void to(T constant) {
			toConstant(constant);
		}

		public final void toMultiple(T constant1, T constant2) {
			onMulti().toConstant(constant1).toConstant(constant2);
		}

		public final void toMultiple(T constant1, T constant2, T constant3) {
			onMulti().toConstant(constant1) //
					.toConstant(constant2) //
					.toConstant(constant3);
		}

		@SafeVarargs
		public final void toMultiple(T constant1, T... constants) {
			TypedBinder<T> multibinder = onMulti().toConstant(constant1);
			for (T constant : constants)
				multibinder.toConstant(constant);
		}

		public void toConstructor() {
			toConstructor(locator.type().rawType);
		}

		@SuppressWarnings("unchecked")
		public void toConstructor(Class<? extends T> impl, Hint<?>... hints) {
			if (!isClassConstructable(impl))
				throw InconsistentDeclaration.notConstructable(impl);
			Constructor<? extends T> target = (Constructor<? extends T>)
					env(ConstructsBy.class) //
						.reflect(impl.getDeclaredConstructors(), hints);
			if (target == null)
				throw InconsistentBinding.generic(
						"No usable Constructor for type: " + impl);
			toConstructor(target, hints);
		}

		public void toConstructor(Hint<?>... hints) {
			toConstructor(getType().rawType, hints);
		}

		@Override
		public final <I extends T> void to(Instance<I> instance) {
			expand(instance);
		}

		public <I extends T> void toParametrized(Class<I> impl) {
			expand(new BridgeDescriptor(impl));
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

		protected final Type<T> getType() {
			return locator.type();
		}

		protected final TypedBinder<T> on(Bind bind) {
			return new TypedBinder<>(bind, locator);
		}

		protected final TypedBinder<T> onMulti() {
			return on(bind().asMulti());
		}

	}

	/**
	 * This kind of bindings actually re-map the []-type so that the automatic
	 * behavior of returning all known instances of the element type will no
	 * longer be used whenever the bind made applies.
	 */
	public static class TypedElementBinder<E> extends TypedBinder<E[]> {

		protected TypedElementBinder(Bind bind, Instance<E[]> instance) {
			super(bind.asMulti().next(), instance);
		}

		@SafeVarargs
		@SuppressWarnings("unchecked")
		public final void toElements(Class<? extends  E>... elems) {
			toElements(stream(elems) //
					.map(e -> relativeReferenceTo(raw(e))) //
					.toArray(Hint[]::new));
		}

		@SafeVarargs
		public final void toElements(Hint<? extends E>... elems) {
			expand(new ArrayDescriptor(elems));
		}

		@SafeVarargs
		public final void toElements(E... constants) {
			to(array((Object[]) constants));
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
}
