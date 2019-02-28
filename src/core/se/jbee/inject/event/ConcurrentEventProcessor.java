/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.event;

import static java.lang.reflect.Proxy.newProxyInstance;
import static se.jbee.inject.Type.returnType;
import static se.jbee.inject.event.EventException.unwrapGet;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
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

	private final static class EventHandler extends WeakReference<Object> {

		/**
		 * How many threads are currently calling one of the handlers methods.
		 */
		final AtomicInteger concurrentUsage = new AtomicInteger(0);

		EventHandler(Object handler) {
			super(handler);
		}

		/**
		 * If the use succeeds (result is true) the end of usage should be
		 * marked by calling {@link #free()} so that this handler can continue
		 * to keep track of the concurrent using threads.
		 * 
		 * @return true if this handler could be reserved for usage by the
		 *         calling thread, else false.
		 */
		boolean allocateFor(Event<?, ?> e) {
			while (true) {
				int currentUsage = concurrentUsage.get();
				if (currentUsage >= e.prefs.maxConcurrentUsage)
					return false;
				boolean success = concurrentUsage.compareAndSet(currentUsage,
						currentUsage + 1);
				if (success)
					return true;
			}
		}

		void free() {
			concurrentUsage.decrementAndGet();
		}
	}

	static final class EventHandlers
			extends ConcurrentLinkedDeque<EventHandler> {

		/**
		 * Tries to find a handler that can be used to process the event. A
		 * successfully received handler has to be marked
		 * {@link #free(EventHandler)} right after usage ends unless its handler
		 * reference became collected. In that case the handler became
		 * out-dated.
		 * 
		 * @return a free handler to use or null if there is no such handler
		 */
		EventHandler allocateFor(Event<?, ?> e) {
			// either we run out of handler to poll
			// or we find a handler to use
			while (true) {
				EventHandler h = pollFirst();
				if (h == null)
					return null;
				if (h.allocateFor(e)) {
					if (h.get() != null)
						return h;
				} else {
					addLast(h);
				}
			}
		}

		void free(EventHandler h) {
			h.free();
			addLast(h);
		}
	}

	private final Map<Class<?>, Object> proxiesByEventType = new ConcurrentHashMap<>();
	private final Map<Class<?>, EventHandlers> handlersByEventType = new ConcurrentHashMap<>();
	private final Map<Class<?>, EventPreferences> prefsByEventType = new ConcurrentHashMap<>();
	private final ExecutorService executor;
	private final EventMirror reflector;

	ConcurrentEventProcessor(EventMirror reflector, ExecutorService executor) {
		this.reflector = reflector;
		this.executor = executor;
	}

	@Override
	public void close() {
		executor.shutdown();
	}

	@Override
	public <E> void await(Class<E> event) throws InterruptedException {
		EventHandlers hs = getHandlers(event);
		if (!hs.isEmpty())
			return;
		synchronized (hs) {
			hs.wait();
		}
	}

	private EventPreferences getPrefs(Class<?> event) {
		return prefsByEventType.computeIfAbsent(event,
				e -> reflector.reflect(e));
	}

	@Override
	public <E> void register(Class<E> event, E handler) {
		if (Proxy.isProxyClass(handler.getClass())
			&& Proxy.getInvocationHandler(
					handler).getClass() == ProxyEventHandler.class) {
			return; // prevent own proxies to be registered as this causes multi-threaded endless loops
		}
		EventHandlers hs = getHandlers(event);
		hs.addFirst(new EventHandler(handler));
		synchronized (hs) {
			hs.notifyAll();
		}
	}

	private <E> EventHandlers getHandlers(Class<E> event) {
		return handlersByEventType.computeIfAbsent(event,
				k -> new EventHandlers());
	}

	@Override
	public <E> void deregister(Class<E> event, E handler) {
		EventHandlers hs = handlersByEventType.get(event);
		if (hs != null && !hs.isEmpty())
			hs.removeIf(h -> h.get() == handler);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> E getProxy(Class<E> event) {
		return (E) proxiesByEventType.computeIfAbsent(event,
				e -> newProxyInstance(e.getClassLoader(), new Class[] { e },
						new ProxyEventHandler<>(e, getPrefs(e), this)));
	}

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
			EventException.unwrap(event, () -> res.get());
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
		ensureNoTimeout(event);
		EventHandlers hs = handlersByEventType.get(event.type);
		if (hs == null)
			throw new EventException(event, null);
		while (true) {
			EventHandler h = hs.allocateFor(event);
			if (h == null)
				throw new EventException(event, null);
			@SuppressWarnings("unchecked")
			E handler = (E) h.get();
			if (handler != null) {
				T res = invoke(event, handler);
				hs.free(h);
				return res;
			}
		}
	}

	private <E, T> T doDispatch(Event<E, T> event) throws EventException {
		ensureNoTimeout(event);
		EventHandlers hs = handlersByEventType.get(event.type);
		if (hs == null || hs.isEmpty())
			return null;
		Iterator<EventHandler> iter = hs.iterator();
		LinkedList<EventHandler> retry = null;
		final BinaryOperator<T> aggregator = event.aggregator;
		T res = null;
		while (iter.hasNext()) {
			EventHandler h = iter.next();
			@SuppressWarnings("unchecked")
			E handler = (E) h.get();
			if (handler != null) {
				if (h.allocateFor(event)) {
					T res1 = invoke(event, handler);
					if (aggregator != null)
						res = res == null ? res1 : aggregator.apply(res, res1);
					h.free();
				} else {
					if (retry == null)
						retry = new LinkedList<>();
					retry.add(h);
				}
			} else {
				iter.remove();
			}
		}
		if (retry != null) {
			while (!retry.isEmpty()) {
				EventHandler h = retry.pollFirst();
				@SuppressWarnings("unchecked")
				E handler = (E) h.get();
				if (handler != null) {
					if (h.allocateFor(event)) {
						T res1 = invoke(event, handler);
						if (aggregator != null)
							res = res == null
								? res1
								: aggregator.apply(res, res1);
						h.free();
					} else {
						retry.addLast(h);
					}
				}
			}
		}
		return res;
	}

	private static <E, T> void ensureNoTimeout(Event<E, T> event)
			throws EventException {
		if (event.isOutdated())
			throw new EventException(event, new TimeoutException());
	}

	@SuppressWarnings("unchecked")
	private static <E, T> T invoke(Event<E, T> event, E listener)
			throws EventException {
		try {
			return (T) event.handler.invoke(listener, event.args);
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
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			return invoke(method, args, returnType(method));
		}

		@SuppressWarnings("unchecked")
		private <T> Object invoke(Method method, Object[] args, Type<T> result)
				throws Throwable {
			Class<T> raw = result.rawType;
			Event<E, T> e = new Event<>(event, prefs, result, method, args,
					defaultAggregator(raw, args));
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
		private static <T> BinaryOperator<T> defaultAggregator(Class<T> raw,
				Object[] args) {
			BinaryOperator<T> aggregator = args != null && args.length > 0
				&& args[args.length - 1] != null
				&& BinaryOperator.class.isAssignableFrom(args.getClass())
					? (BinaryOperator<T>) args[args.length - 1]
					: null;
			if (aggregator != null)
				return aggregator;
			if (raw == boolean.class || raw == Boolean.class) {
				return (BinaryOperator<T>) ((BinaryOperator<Boolean>) (a,
						b) -> a || b);
			}
			if (raw == int.class || raw == Integer.class) {
				return (BinaryOperator<T>) ((BinaryOperator<Integer>) (a,
						b) -> a + b);
			}
			return null;
		}

	}
}
