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
import se.jbee.inject.config.Invoke;
import se.jbee.inject.config.ProducesBy;
import se.jbee.lang.Type;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.action.Action.actionTypeOf;
import static se.jbee.inject.binder.spi.ConnectorBinder.ACTION_CONNECTOR;
import static se.jbee.lang.Type.actualReturnType;
import static se.jbee.lang.Type.raw;

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

	public static final class ActionBaseModule extends BinderModule {

		@Override
		public void declare() {
			construct(ActionSupplier.class);
			asDefault().per(Scope.dependencyType) //
					.starbind(Action.class) //
					.toSupplier(ActionSupplier.class);
			asDefault().bind(ACTION_CONNECTOR, Connector.class) //
					.to(ActionSupplier.class);
			asDefault().per(Scope.application) //
					.bind(ActionExecutor.class) //
					.to(this::run);

			asDefault().bind(ActionDispatch.class)
					.to(RoundRobinDispatch.class);
			asDefault().injectingInto(actionTypeOf(Type.WILDCARD, Type.VOID)) //
					.bind(ActionDispatch.class) //
					.to(MulticastDispatch.class);
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
		private final Map<Type<?>, Set<ActionSite.ActionTarget>> targetsByReturnType = new ConcurrentHashMap<>();

		/**
		 * All already created {@link Action}s identified by a unique function
		 * signature.
		 */
		private final Map<String, Action<?, ?>> actionsBySignature = new ConcurrentHashMap<>();

		private final AtomicInteger connectedCount = new AtomicInteger();

		private final Injector context;

		public ActionSupplier(Injector context) {
			this.context = context;
		}

		@Override
		public void connect(Object instance, Type<?> as, Method connected) {
			Invoke invoke = context.resolve(dependency(Invoke.class) //
					.injectingInto(connected.getDeclaringClass()));
			targetsByReturnType.computeIfAbsent(
					actualReturnType(connected, as),
					key -> ConcurrentHashMap.newKeySet()).add(
					new ActionSite.ActionTarget(instance, as, connected, invoke));
			connectedCount.incrementAndGet();
		}

		@Override
		public Action<?, ?> supply(Dependency<? super Action<?, ?>> dep,
				Injector context) {
			Type<? super Action<?, ?>> type = dep.type();
			return provide(type.parameter(0), type.parameter(1), context);
		}

		@SuppressWarnings("unchecked")
		private <A, B> Action<A, B> provide(Type<A> in, Type<B> out,
				Injector context) {
			return (Action<A, B>) actionsBySignature.computeIfAbsent(
					getSignature(in, out), key -> newAction(in, out, context));
		}

		private <A, B> String getSignature(Type<A> in, Type<B> out) {
			return in + "->" + out;
		}

		private <A, B> Action<A, B> newAction(Type<A> in, Type<B> out,
				Injector context) {
			AtomicReference<List<ActionSite<A,B>>> cache = new AtomicReference<>();
			AtomicInteger cachedAtCount = new AtomicInteger();
			@SuppressWarnings("unchecked")
			ActionDispatch<A, B> strategy = context.resolve(dependency(
					raw(ActionDispatch.class).parameterized(in, out))
					.injectingInto(actionTypeOf(in, out)));
			return input -> {
				List<ActionSite<A, B>> sites = cache.updateAndGet(list -> {
					int count = connectedCount.get();
					if (list != null && cachedAtCount.get() == count)
						return list;
					cachedAtCount.set(count);
					return resolveActions(in, out, context, cachedAtCount);
				});
				try {
					return strategy.execute(input, sites);
				} catch (DisconnectException ex) {
					throw new NoMethodForDependency(out, in);
				}
			};
		}

		private <A, B> List<ActionSite<A, B>> resolveActions(Type<A> in,
				Type<B> out, Injector context, AtomicInteger cachedAtCount) {
			Set<ActionSite.ActionTarget> targets = targetsByReturnType.get(out);
			if (targets == null)
				return emptyList();
			List<ActionSite<A, B>> matching = new ArrayList<>();
			for (ActionSite.ActionTarget candidate : targets) {
				if (candidate.isUsableFor(in, out))
					matching.add(new ActionSite<>(candidate, in, out, context,
							site -> {
								targets.remove(candidate);
								cachedAtCount.set(0); // invalidate cache
							}));
			}
			return matching.isEmpty()
					? emptyList()
					: unmodifiableList(matching);
		}
	}
}
