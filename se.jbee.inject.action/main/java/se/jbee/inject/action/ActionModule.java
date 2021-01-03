/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.action;

import se.jbee.inject.*;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;
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
			methodsByReturnType.computeIfAbsent(
					actualReturnType(connected, as),
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
			return new LazyAction<>(in, out, injector, executor, () -> {
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
				return emptyList();
			//OBS! It is important to use a LinkedList here as it allows to iterate and remove elements at the same time
			List<ActionSite<A,B>> matching = new ArrayList<>();
			for (ActionSite.ActionTarget candidate : targets) {
				if (candidate.isApplicableFor(in, out))
					matching.add(new ActionSite<>(candidate, in, out, injector,
							site -> {
								targets.remove(candidate);
								matching.remove(site);
							}));
			}
			return matching.isEmpty()
					? emptyList()
					: new CopyOnWriteArrayList<>(matching);
		}

	}

	private static final class LazyAction<A, B> implements Action<A, B> {

		private final Type<A> in;
		private final Type<B> out;
		private final Injector context;
		private final Executor executor;
		private final java.util.function.Supplier<List<ActionSite<A, B>>> sites;
		private final AtomicInteger nextSite = new AtomicInteger();
		private final BiFunction<List<ActionSite<A, B>>, A, B> router;

		LazyAction(Type<A> in, Type<B> out, Injector context, Executor executor,
				java.util.function.Supplier<List<ActionSite<A, B>>> sites) {
			this.in = in;
			this.out = out;
			this.context = context;
			this.executor = executor;
			this.sites = sites;
			this.router = out.rawType == void.class || out.rawType == Void.class
					? this::multicast
					: this::roundRobin;
		}

		@Override
		public B run(A input) throws ActionExecutionFailed {
			return router.apply(this.sites.get(), input);
		}

		private B multicast(List<ActionSite<A, B>> activeSites, A input) {
			ActionExecutionFailed ex = null;
			int disconnected = 0;
			for (ActionSite<A, B> site : activeSites) {
				try {
					executor.run(site, site.args(context, input), input);
				} catch (DisconnectException e) {
					// not incrementing the index as element at that index now is the next in line
					disconnected++;
				} catch (ActionExecutionFailed e) {
					ex = e;
				}
			}
			if (activeSites.size() <= disconnected)
				throw new NoMethodForDependency(out, in);
			if (ex != null)
				throw ex;
			return null;
		}

		private B roundRobin(List<ActionSite<A, B>> activeSites, A input) {
			int attempts = activeSites.size();
			while (attempts > 0) {
				int i = nextSite.getAndIncrement();
				ActionSite<A, B> site = activeSites.get(i % activeSites.size());
				try {
					return executor.run(site, site.args(context, input), input);
				} catch (DisconnectException ex) {
					// test the next
					attempts--;
				}
			}
			throw new NoMethodForDependency(out, in);
		}

		@Override
		public String toString() {
			return "LazyAction[" + in + " => " + out + "]";
		}
	}
}
