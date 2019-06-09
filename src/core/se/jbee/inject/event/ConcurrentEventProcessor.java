/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.event;

import static java.lang.reflect.Proxy.newProxyInstance;
import static se.jbee.inject.Type.returnType;
import static se.jbee.inject.event.EventException.unwrapGet;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;

import se.jbee.inject.Type;

/**
 * Default implementation of the {@link EventProcessor} supporting all
 * {@link EventPreferences}.
 * 
 * @since 19.1
 */
public class ConcurrentEventProcessor implements EventProcessor {

	private static final class EventHandler<E> {

		/**
		 * How many threads are currently calling one of the handlers methods.
		 */
		final AtomicInteger concurrentCalls = new AtomicInteger(0);
		final E handler;

		EventHandler(E handler) {
			this.handler = handler;
		}

		/**
		 * If the use succeeds (result is true) the end of usage should be
		 * marked by calling {@link #release()} so that this handler can
		 * continue to keep track of the concurrent using threads.
		 * 
		 * @return true if this handler could be reserved for usage by the
		 *         calling thread, else false.
		 */
		boolean acquire(Event<?, ?> e) {
			while (true) {
				int calls = concurrentCalls.get();
				if (calls >= e.prefs.maxConcurrency)
					return false;
				if (concurrentCalls.compareAndSet(calls, calls + 1))
					return true;
			}
		}

		void release() {
			concurrentCalls.decrementAndGet();
		}
	}

	static final class EventHandlers<E>
			extends ConcurrentLinkedDeque<EventHandler<E>> {

		/**
		 * Tries to find a handler that can be used to process the event. A
		 * successfully received handler has to be marked
		 * {@link #release(EventHandler)} right after usage ends unless its
		 * handler reference became collected. In that case the handler became
		 * out-dated.
		 * 
		 * @return a free handler to use or null if there is no such handler
		 */
		EventHandler<E> acquire(Event<E, ?> e) {
			// either we run out of handler to poll
			// or we find a handler to use
			while (true) {
				EventHandler<E> h = pollFirst();
				if (h == null)
					return null;
				if (h.acquire(e)) {
					return h;
				}
				addLast(h);
			}
		}

		void release(EventHandler<E> h) {
			h.release();
			addLast(h);
		}
	}

	private final Map<Class<?>, Object> proxiesByEventType = new ConcurrentHashMap<>();
	private final Map<Class<?>, EventHandlers<?>> handlersByEventType = new ConcurrentHashMap<>();
	private final Map<Class<?>, EventPreferences> prefsByEventType = new ConcurrentHashMap<>();
	private final ExecutorService executor;
	private final EventMirror mirror;

	ConcurrentEventProcessor(EventMirror mirror, ExecutorService executor) {
		this.mirror = mirror;
		this.executor = executor;
	}

	@Override
	public void close() {
		executor.shutdown();
	}

	@Override
	public <E> void await(Class<E> event) throws InterruptedException {
		EventHandlers<E> hs = getHandlers(event, true);
		if (!hs.isEmpty())
			return;
		synchronized (hs) {
			hs.wait();
		}
	}

	private EventPreferences getPrefs(Class<?> event) {
		return prefsByEventType.computeIfAbsent(event, mirror::reflect);
	}

	@Override
	public <E> void register(Class<E> event, E handler) {
		if (Proxy.isProxyClass(handler.getClass())
			&& Proxy.getInvocationHandler(
					handler).getClass() == ProxyEventHandler.class) {
			return; // prevent own proxies to be registered as this causes multi-threaded endless loops
		}
		EventHandlers<E> hs = getHandlers(event, true);
		hs.addFirst(new EventHandler<>(handler));
		synchronized (hs) {
			hs.notifyAll();
		}
	}

	@SuppressWarnings("unchecked")
	private <E> EventHandlers<E> getHandlers(Class<E> event, boolean init) {
		return (EventHandlers<E>) (init
			? handlersByEventType.computeIfAbsent(event,
					k -> new EventHandlers<>())
			: handlersByEventType.get(event));
	}

	@Override
	public <E> void deregister(Class<E> event, E handler) {
		EventHandlers<?> hs = handlersByEventType.get(event);
		if (hs != null && !hs.isEmpty())
			hs.removeIf(h -> h.handler == handler);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> E getProxy(Class<E> event) {
		return (E) proxiesByEventType.computeIfAbsent(event,
				e -> newProxyInstance(e.getClassLoader(), new Class[] { e },
						new ProxyEventHandler<>(e, getPrefs(e), this)));
	}

	//TODO event processing has to be done in a way that makes a try for each target handler - should the event not be completely handled it has to requeue for retries so that other events are processed in the meantime
	private <T> Future<T> submit(Event<?, ?> event, Callable<T> f) {
		try {
			return executor.submit(f);
		} catch (RejectedExecutionException e) {
			throw new EventException(event, e);
		}
	}

	@Override
	public <E> void dispatch(Event<E, ?> event) throws Throwable {
		Future<?> res;
		if (event.prefs.isMultiDispatch()) {
			res = submit(event, () -> doDispatch(event));
		} else {
			res = submit(event, () -> doCompute(event));
		}
		if (event.prefs.isSyncMultiDispatch())
			EventException.unwrap(event, res::get);
	}

	// - when should I give up?
	// - what to do when giving up?
	// - how often should I retry?
	// note: these can be programmed as a utility on top with basically the same effect and little extra overhead for a corner case
	//       as long as there is a clear contract: namely that failure is always indicated by a EventEception
	@Override
	public <E, T> T compute(Event<E, T> event) throws Throwable {
		return unwrapGet(event, submit(event, () -> doProcess(event)));
	}

	@Override
	public <E, T extends Future<V>, V> Future<V> computeEventually(
			Event<E, T> event) {
		return new UnboxingFuture<>(event,
				submit(event, () -> doProcess(event)));
	}

	private <E, T> T doProcess(Event<E, T> event) throws EventException {
		return event.prefs.isAggregatedMultiDispatch()
			&& event.aggregator != null ? doDispatch(event) : doCompute(event);
	}

	private <E, T> T doCompute(Event<E, T> event) throws EventException {
		ensureNotExpired(event);
		EventHandlers<E> hs = getHandlers(event.type, false);
		if (hs == null)
			throw new EventException(event, null);
		EventHandler<E> h = hs.acquire(event);
		if (h == null)
			throw new EventException(event, null); //TODO this should maybe become a Unsupported exception?
		try {
			return doHandle(event, h.handler);
		} finally {
			hs.release(h);
		}
	}

	private <E, T> T doDispatch(Event<E, T> event) throws EventException {
		ensureNotExpired(event);
		EventHandlers<E> hs = getHandlers(event.type, false);
		if (hs == null || hs.isEmpty())
			return null;
		LinkedList<EventHandler<E>> needRetry = null;
		T res = null;
		for (EventHandler<E> h : hs) {
			if (h.acquire(event)) {
				res = doAggregate(event, h, res);
			} else {
				if (needRetry == null)
					needRetry = new LinkedList<>();
				needRetry.add(h);
			}
		}
		if (needRetry != null && !needRetry.isEmpty()) {
			for (int i = 0; i < event.prefs.maxRetries; i++) {
				int size = needRetry.size();
				for (int j = 0; j < size; j++) {
					EventHandler<E> h = needRetry.pollFirst();
					if (h.acquire(event)) {
						res = doAggregate(event, h, res);
					} else {
						needRetry.addLast(h);
					}
				}
			}
			if (needRetry.isEmpty())
				return res;
		}
		return res;
	}

	private static <T, E> T doAggregate(Event<E, T> event, EventHandler<E> h,
			T res) {
		final BinaryOperator<T> aggregator = event.aggregator;
		try {
			T res1 = doHandle(event, h.handler);
			return aggregator == null || res == null
				? res1
				: aggregator.apply(res, res1);
			// TODO shoudn't there be a catch here so each handler's errors are isolated?
		} finally {
			h.release();
		}
	}

	private static <E, T> void ensureNotExpired(Event<E, T> event)
			throws EventException {
		if (event.isExpired())
			throw new EventException(event, new TimeoutException());
	}

	@SuppressWarnings("unchecked")
	private static <E, T> T doHandle(Event<E, T> event, E handler)
			throws EventException {
		try {
			return (T) event.target.invoke(handler, event.args);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new EventException(event, e);
		}
	}

	static class ProxyEventHandler<E> implements InvocationHandler {

		final Class<E> event;
		final EventPreferences prefs;
		final EventProcessor processor;

		public ProxyEventHandler(Class<E> event, EventPreferences prefs,
				EventProcessor processor) {
			this.event = event;
			this.prefs = prefs;
			this.processor = processor;
		}

		@Override
		public Object invoke(Object proxy, Method target, Object[] args)
				throws Throwable {
			return invoke(target, args, returnType(target));
		}

		@SuppressWarnings("unchecked")
		private <T> Object invoke(Method target, Object[] args, Type<T> result)
				throws Throwable {
			Class<T> raw = result.rawType;
			Event<E, T> e = new Event<>(event, prefs, result, target, args,
					(BinaryOperator<T>) defaultAggregator(raw,
							target.getParameterTypes(), args));
			if (e.returnsVoid()) {
				processor.dispatch((Event<E, Void>) e);
				return null;
			}
			if (raw == Future.class) {
				return processor.computeEventually(
						(Event<E, Future<Object>>) e);
			}
			return processor.compute(e);
		}

		@SuppressWarnings("unchecked")
		private static <T> BinaryOperator<?> defaultAggregator(
				Class<T> rawReturnType, Class<?>[] parameterTypes,
				Object[] args) {
			BinaryOperator<T> aggregator = args != null && args.length > 0
				&& args[args.length - 1] != null
				&& BinaryOperator.class.isAssignableFrom(
						parameterTypes[args.length - 1])
							? (BinaryOperator<T>) args[args.length - 1]
							: null;
			if (aggregator != null)
				return aggregator;
			if (rawReturnType == boolean.class
				|| rawReturnType == Boolean.class)
				return ((BinaryOperator<Boolean>) Boolean::logicalAnd);
			if (rawReturnType == int.class || rawReturnType == Integer.class)
				return ((BinaryOperator<Integer>) Integer::sum);
			return null;
		}

	}
}
