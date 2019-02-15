package se.jbee.inject.event;

import static java.lang.reflect.Proxy.newProxyInstance;
import static se.jbee.inject.Type.returnType;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import se.jbee.inject.Type;
import se.jbee.inject.event.EventHandlerReflector.EventHandlerProperties;

public class ConcurrentEventProcessor implements EventProcessor {

	private final static class EventHandler extends WeakReference<Object> {
 
		/**
		 * How many threads are allowed to call any of the handlers methods
		 * concurrently.
		 */
		final EventHandlerProperties properties;
		/**
		 * How many threads are currently calling one of the handlers methods.
		 */
		final AtomicInteger concurrentUsage = new AtomicInteger(0);
		
		EventHandler(Object handler, EventHandlerProperties properties) {
			super(handler);
			this.properties = properties;
		}
		
		/**
		 * If the use succeeds (result is true) the end of usage should be marked by
		 * calling {@link #unuse()} so that this handler can continue to keep track of
		 * the concurrent using threads.
		 * 
		 * @return true if this handler could be reserved for usage by the calling
		 *         thread, else false.
		 */
		boolean use() {
			while (true) {
				int currentUsage = concurrentUsage.get();
				if (currentUsage >= properties.maxConcurrentUsage)
					return false;
				boolean success = concurrentUsage.compareAndSet(currentUsage, currentUsage + 1);
				if (success)
					return true;
			}
		}
		
		void unuse() {
			concurrentUsage.decrementAndGet();
		}
	}
	
	static final class EventHandlers extends ConcurrentLinkedDeque<EventHandler> {
		
		/**
		 * Tries to find a handler that can be used to process the event. A successfully
		 * received handler has to be marked {@link #done(EventHandler)} right after
		 * usage ends unless its handler reference became collected. In that case the
		 * handler became out-dated.
		 * 
		 * @return a free handler to use or null if there is no such handler
		 */
		EventHandler next() {
			// either we run out of handler to poll
			// or we find a handler to use
			while (true) {
				EventHandler h = pollFirst();
				if (h == null) 
					return null;
				if (h.use()) {
					if (h.get() != null)					
						return h;
				} else {
					addLast(h);
				}
			}
		}
		
		void done(EventHandler h) {
			h.unuse();
			addLast(h);
		}
	}
	
	private final Map<Class<?>, Object> proxies = new ConcurrentHashMap<>();
	private final Map<Class<?>, EventHandlers> handlers = new ConcurrentHashMap<>();
	
	//TODO handle shutdown properly
	private final ExecutorService executor = Executors.newWorkStealingPool();
	private final EventHandlerReflector reflector;
	
	ConcurrentEventProcessor(EventHandlerReflector reflector) {
		this.reflector = reflector;
	}
	
	@Override
	public <E> void await(Class<E> event) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public <E> void register(Class<E> event, E handler) {
		if (Proxy.isProxyClass(handler.getClass()) 
				&& Proxy.getInvocationHandler(handler).getClass() == ProxyEventHandler.class) {
			return; // prevent own proxies to be registered as this causes multi-threaded endless loops
		}
		handlers.computeIfAbsent(event, k -> new EventHandlers()).addFirst(
				new EventHandler(handler, reflector.getProperties(event, handler)));
	}
	
	@Override
	public <E> void deregister(Class<E> event, E handler) {
		EventHandlers hs = handlers.get(event);
		if (hs != null && !hs.isEmpty()) {
			hs.removeIf(h -> h.get() == handler);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> E getProxy(Class<E> event) {
		return (E) proxies.computeIfAbsent(event, e -> 
			newProxyInstance(e.getClassLoader(), new Class[] { e }, 
					new ProxyEventHandler<>(e, this)));
	}
	
	@Override
	public <E, T> void dispatch(Event<E, T> event) {
		executor.submit(() -> doDispatch(event));
	}

	// - when should I give up?
	// - what to do when giving up? 
	// - how often should I retry?
	// note: these can be programmed as a utility on top with basically the same effect and little extra overhead for a corner case
	//       as long as there is a clear contract: namely that failure is always indicated by a EventEception
	@Override
	public <E, T> T compute(Event<E, T> event) {
		try {
			return executor.submit(() -> doCompute(event)).get();
		} catch (ExecutionException e) {
			if (e.getCause() instanceof EventException) {
				throw (EventException)e.getCause();
			}
			throw new EventException(e);
		} catch (InterruptedException e) {
			throw new EventException(e);
		}
	}
	
	@Override
	public <E, T extends Future<V>, V> Future<V> computeEventually(Event<E, T> event) {
		return new UnboxingFuture<>(executor.submit(() -> doCompute(event)));
	}

	private <E, T> T doCompute(Event<E, T> event) {
		EventHandlers hs = handlers.get(event.type);
		if (hs == null)
			throw new EventException(null);
		while (true) {
			EventHandler h = hs.next();
			if (h == null)
				throw new EventException(null);
			@SuppressWarnings("unchecked")
			E handler = (E) h.get();
			if (handler != null) {
				T res = invoke(event, handler);
				hs.done(h);
				return res;
			}
		}
	}
	
	private <E, T> void doDispatch(Event<E, T> event) {
		EventHandlers hs = handlers.get(event.type);
		if (hs == null || hs.isEmpty())
			return;
		Iterator<EventHandler> iter = hs.iterator();
		LinkedList<EventHandler> retry = null;
		while (iter.hasNext()) {
			EventHandler h = iter.next();
			@SuppressWarnings("unchecked")
			E handler = (E) h.get();
			if (handler != null) {
				if (h.use()) {
					invoke(event, handler);
					h.unuse();
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
					if (h.use()) {
						invoke(event, handler);
						h.unuse();
					} else {
						retry.addLast(h);
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <E, T> T invoke(Event<E, T> event, E listener) {
		try {
			return (T) event.handler.invoke(listener, event.args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new EventException(e);
		}
	}
	
	static class ProxyEventHandler<E> implements InvocationHandler {

		final Class<E> event;
		final EventProcessor processor;

		public ProxyEventHandler(Class<E> event, EventProcessor processor) {
			this.event = event;
			this.processor = processor;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return invoke(method, args, returnType(method));
		}
		
		@SuppressWarnings("unchecked")
		private <T> Object invoke(Method method, Object[] args, Type<T> result) {
			Class<T> raw = result.rawType;
			Event<E, T> e = new Event<>(event, result, method, args);
			if (raw == void.class || raw == Void.class) {
				processor.dispatch(e);
				return null;
			}
			if (raw == Future.class) {
				return processor.computeEventually((Event<E, Future<Object>>)e);
			}
			return processor.compute(e);
		}
		
	}
}
