/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.action;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.Supplier;
import se.jbee.inject.UnresolvableDependency.NoMethodForDependency;
import se.jbee.inject.bind.Module;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.config.Connector;
import se.jbee.inject.config.ProducesBy;
import se.jbee.inject.lang.Type;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static se.jbee.inject.lang.Type.actualReturnType;

/**
 * Base {@link Module} that needs to be extended and installed at least once to
 * add the general setup for {@link Action}s.
 * <p>
 * To add {@link Method}s as {@link Action}s use {@link #connect(ProducesBy)}
 * and {@link ConnectTargetBinder#asAction()} as in this example:
 *
 * <pre>
 * connect(ProducesBy.allMethods).in(MyService.class).asAction();
 * </pre>
 */
public abstract class ActionModule extends BinderModule {

	protected ActionModule() {
		super(ActionBaseModule.class);
	}

	private static final class ActionBaseModule extends BinderModule {

		@Override
		public void declare() {
			construct(ActionSupplier.class);
			asDefault().per(Scope.dependencyType) //
					.starbind(Action.class).toSupplier(ActionSupplier.class);
			asDefault().bind(ACTION_CONNECTOR, Connector.class) //
					.to(ActionSupplier.class);
			asDefault().per(Scope.application) //
					.bind(Executor.class) //
					.to(this::run);
		}

		<A, B> B run(ActionSite<A, B> site, Object[] args, A value) {
			return site.call(args, null);
		}
	}

	public static final class ActionSupplier implements Supplier<Action<?, ?>>,
			Connector {

		/**
		 * A list of discovered methods for each implementation class.
		 */
		private final Map<Type<?>, Set<ActionSite.ActionTarget>> methodsByReturnType = new ConcurrentHashMap<>();
		/**
		 * All already created {@link Action}s identified by a unique function
		 * signature.
		 */
		private final Map<String, Action<?, ?>> actionsBySignature = new ConcurrentHashMap<>();

		private final Injector injector;
		private final Executor executor;
		private final AtomicInteger connectedCount = new AtomicInteger();

		public ActionSupplier(Injector injector) {
			this.injector = injector;
			this.executor = injector.resolve(Executor.class);
		}

		@Override
		public void connect(Object instance, Type<?> as, Method connected) {
			methodsByReturnType.computeIfAbsent(actualReturnType(connected, as),
					key -> ConcurrentHashMap.newKeySet()).add(
					new ActionSite.ActionTarget(instance, as, connected));
			connectedCount.incrementAndGet();
		}

		@Override
		public Action<?, ?> supply(Dependency<? super Action<?, ?>> dep,
				Injector context) {
			Type<? super Action<?, ?>> type = dep.type();
			return provide(type.parameter(0), type.parameter(1));
		}

		@SuppressWarnings("unchecked")
		private <A, B> Action<A, B> provide(Type<A> in, Type<B> out) {
			return (Action<A, B>) actionsBySignature.computeIfAbsent(
					getSignature(in, out), key -> newAction(in, out));
		}

		private <A, B> String getSignature(Type<A> in, Type<B> out) {
			return in + "->" + out;
		}

		private <A, B> Action<?, ?> newAction(Type<A> in, Type<B> out) {
			AtomicReference<List<ActionSite<A,B>>> cache = new AtomicReference<>();
			AtomicInteger cachedAtConnectionCount = new AtomicInteger();
			return new LazyAction<>(injector, executor, () -> {
				if (cachedAtConnectionCount.get() < connectedCount.get()) {
					cache.set(null);
					cachedAtConnectionCount.set(connectedCount.get());
				}
				return cache.updateAndGet(list -> list != null ? list : resolveActions(in, out));
			});
		}

		private <A, B> List<ActionSite<A,B>> resolveActions(Type<A> in, Type<B> out) {
			Set<ActionSite.ActionTarget> targets = methodsByReturnType.get(out);
			if (targets == null)
				throw new NoMethodForDependency(out, in);
			List<ActionSite<A,B>> matching = new ArrayList<>(1);
			for (ActionSite.ActionTarget candidate : targets) {
				if (candidate.isApplicableFor(in, out))
					matching.add(new ActionSite<>(candidate, in, out, injector));
			}
			if (matching.isEmpty())
				throw new NoMethodForDependency(out, in);
			return matching;
		}

	}

	private static final class LazyAction<A, B> implements Action<A, B> {

		private final Injector context;
		private final Executor executor;
		private final java.util.function.Supplier<List<ActionSite<A, B>>> sites;

		LazyAction(Injector context, Executor executor,
				java.util.function.Supplier<List<ActionSite<A, B>>> sites) {
			this.context = context;
			this.executor = executor;
			this.sites = sites;
		}

		@Override
		public B run(A input) throws ActionExecutionFailed {
			List<ActionSite<A, B>> activeSites = this.sites.get();
			if (activeSites.size() == 1)
				return executor.run(activeSites.get(0),
						activeSites.get(0).args(context, input), input);
			for (ActionSite<A, B> site : activeSites)
				executor.run(site, site.args(context, input), input);
			return null;
		}
	}
}
