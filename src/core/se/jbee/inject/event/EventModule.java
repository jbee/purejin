/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.bootstrap.Module;
import se.jbee.inject.container.Initialiser;

/**
 * Base {@link Module} for modules that want to make known a handler to the
 * event system using {@link #handle(Class)}.
 * 
 * @since 19.1
 */
public abstract class EventModule extends BinderModule {

	protected EventModule() {
		super(EventBaseModule.class);
	}

	/**
	 * Registers the given event type so that it is handled by the
	 * {@link EventProcessor} system.
	 * 
	 * That means classes implementing the given event interface "automatically"
	 * receive calls to any of the interface methods. When The event interface
	 * should be injected to signal/call one of its methods a
	 * {@link EventProcessor#getProxy(Class)} is injected.
	 * 
	 * @param event the type of the event/listener (must be an interface)
	 */
	protected <T> void handle(Class<T> event) {
		if (!event.isInterface())
			throw new IllegalArgumentException(
					"Event type has to be an interface but was: " + event);
		initbind(event).to((Initialiser<T>) (listener,
				injector) -> injector.resolve(EventProcessor.class).register(
						event, listener));
		bind(event).toSupplier((dep,
				context) -> context.resolve(EventProcessor.class).getProxy(
						event));
	}

	private static final class EventBaseModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault().bind(EventProcessor.class).to(
					ConcurrentEventProcessor.class);
			asDefault().bind(EventMirror.class).to(
					event -> EventPreferences.DEFAULT);
			asDefault().injectingInto(EventProcessor.class).bind(
					ExecutorService.class).to(Executors::newWorkStealingPool);
		}

	}

}
