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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import se.jbee.inject.Type;

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
		 * If the use succeeds (result is true) the end of usage should be marked by
		 * calling {@link #free()} so that this handler can continue to keep track of
		 * the concurrent using threads.
		 * 
		 * @return true if this handler could be reserved for usage by the calling
		 *         thread, else false.
		 */
		boolean allocateFor(Event<?, ?> e) {
			while (true) {
				int currentUsage = concurrentUsage.get();
				if (currentUsage >= e.properties.maxConcurrentUsage)
					return false;
				boolean success = concurrentUsage.compareAndSet(currentUsage, currentUsage + 1);
				if (success)
					return true;
			}
		}
		
		void free() {
			concurrentUsage.decrementAndGet();
		}
	}
	
	static final class EventHandlers extends ConcurrentLinkedDeque<EventHandler> {
		
		/**
		 * Tries to find a handler that can be used to process the event. A successfully
		 * received handler has to be marked {@link #free(EventHandler)} right after
		 * usage ends unless its handler reference became collected. In that case the
		 * handler became out-dated.
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
	
	private final Map<Class<?>, Object> proxies = new ConcurrentHashMap<>();
	private final Map<Class<?>, EventHandlers> handlers = new ConcurrentHashMap<>();
	private final Map<Class<?>, EventProperties> properties = new ConcurrentHashMap<>();
	private final ExecutorService executor;
	private final EventReflector reflector;
	
	ConcurrentEventProcessor(EventReflector reflector, ExecutorService executor) {
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
	
	private EventProperties getProperties(Class<?> event) {
		return properties.computeIfAbsent(event, e -> reflector.reflect(e));
	}
	
	@Override
	public <E> void register(Class<E> event, E handler) {
		if (Proxy.isProxyClass(handler.getClass()) 
				&& Proxy.getInvocationHandler(handler).getClass() == ProxyEventHandler.class) {
			return; // prevent own proxies to be registered as this causes multi-threaded endless loops
		}
		EventHandlers hs = getHandlers(event);
		hs.addFirst(new EventHandler(handler));
		synchronized (hs) {
			hs.notifyAll();
		}
	}

	private <E> EventHandlers getHandlers(Class<E> event) {
		return handlers.computeIfAbsent(event, k -> new EventHandlers());
	}
	
	@Override
	public <E> void deregister(Class<E> event, E handler) {
		EventHandlers hs = handlers.get(event);
		if (hs != null && !hs.isEmpty())
			hs.removeIf(h -> h.get() == handler);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> E getProxy(Class<E> event) {
		return (E) proxies.computeIfAbsent(event, e -> 
			newProxyInstance(e.getClassLoader(), new Class[] { e }, 
					new ProxyEventHandler<>(e, getProperties(e), this)));
	}

	@Override
	public <E> void dispatch(Event<E, ?> event) {
		Future<?> res;
		if (event.properties.multiDispatchVoids) {
			res = executor.submit((Runnable)() -> doDispatch(event));
		} else {
			res = executor.submit(() -> doCompute(event));
		}
		if (event.properties.synchroniseVoids) {
			try {
				EventException.unwrap(event, () -> res.get());
			} catch (EventException e) {
				throw e;
			} catch (Throwable e) {
				if (e instanceof Exception)
					throw new EventException(event, (Exception) e);
				throw new RuntimeException(e);
			}
		}
	}

	// - when should I give up?
	// - what to do when giving up? 
	// - how often should I retry?
	// note: these can be programmed as a utility on top with basically the same effect and little extra overhead for a corner case
	//       as long as there is a clear contract: namely that failure is always indicated by a EventEception
	@Override
	public <E, T> T compute(Event<E, T> event) throws Throwable {
		if (event.returnsBoolean() && event.properties.multiDispatchBooleans) {
			@SuppressWarnings("unchecked")
			T res = unwrapGet(event, executor.submit(() -> (T) Boolean.valueOf(doDispatch(event) > 0)));
			return res;
		}
		return unwrapGet(event, executor.submit(() -> doCompute(event)));
	}
	
	@Override
	public <E, T extends Future<V>, V> Future<V> computeEventually(Event<E, T> event) {
		return new UnboxingFuture<>(event, executor.submit(() -> doCompute(event)));
	}

	private <E, T> T doCompute(Event<E, T> event) throws EventException {
		ensureNoTimeout(event);
		EventHandlers hs = handlers.get(event.type);
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
	
	private <E, T> int doDispatch(Event<E, T> event) throws EventException {
		ensureNoTimeout(event);
		EventHandlers hs = handlers.get(event.type);
		if (hs == null || hs.isEmpty())
			return 0;
		Iterator<EventHandler> iter = hs.iterator();
		LinkedList<EventHandler> retry = null;
		int c = 0;
		while (iter.hasNext()) {
			EventHandler h = iter.next();
			@SuppressWarnings("unchecked")
			E handler = (E) h.get();
			if (handler != null) {
				if (h.allocateFor(event)) {
					if (invoke(event, handler) == Boolean.TRUE)
						c++;
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
						if (invoke(event, handler) == Boolean.TRUE)
							c++;
						h.free();
					} else {
						retry.addLast(h);
					}
				}
			}
		}
		return c;
	}

	private static <E, T> void ensureNoTimeout(Event<E, T> event) throws EventException {
		if (event.isOutdated())
			throw new EventException(event, new TimeoutException());
	}

	@SuppressWarnings("unchecked")
	private static <E, T> T invoke(Event<E, T> event, E listener) throws EventException {
		try {
			return (T) event.handler.invoke(listener, event.args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new EventException(event, e);
		}
	}
	
	static class ProxyEventHandler<E> implements InvocationHandler {

		final Class<E> event;
		final EventProperties properties;
		final EventProcessor processor;

		public ProxyEventHandler(Class<E> event, EventProperties properties, EventProcessor processor) {
			this.event = event;
			this.properties = properties;
			this.processor = processor;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return invoke(method, args, returnType(method));
		}
		
		@SuppressWarnings("unchecked")
		private <T> Object invoke(Method method, Object[] args, Type<T> result) throws Throwable {
			Class<T> raw = result.rawType;
			Event<E, T> e = new Event<>(event, properties, result, method, args);
			if (e.returnsVoid()) {
				processor.dispatch((Event<E, Void>)e);
				return null;
			}
			if (raw == Future.class) {
				return processor.computeEventually((Event<E, Future<Object>>)e);
			}
			return processor.compute(e);
		}
		
	}
}
