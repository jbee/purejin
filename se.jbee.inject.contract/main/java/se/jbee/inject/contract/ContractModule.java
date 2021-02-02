/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.contract;

import se.jbee.inject.bind.Module;
import se.jbee.inject.binder.BinderModule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Base {@link Module} for modules that want to make known a handler to the
 * event system using {@link #handle(Class)}.
 *
 * @since 8.1
 */
public abstract class ContractModule extends BinderModule {

	protected ContractModule() {
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
	 * @param handlerType the type of the event/listener (must be an interface)
	 */
	protected <T> void handle(Class<T> handlerType) {
		if (!handlerType.isInterface())
			throw new IllegalArgumentException(
					"Event type has to be an interface but was: " + handlerType);
		lift(handlerType).to((listener, as, context) -> {
			context.resolve(EventProcessor.class).register(handlerType, listener);
			return listener;
		});
		bind(handlerType).toSupplier((dep, context) -> //
			context.resolve(EventProcessor.class).getProxy(handlerType));
	}

	public static final class EventBaseModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault().bind(EventProcessor.class).to(
					ConcurrentEventProcessor.class);
			asDefault().bind(PolicyProvider.class).to(
					handlerType -> EventPolicy.DEFAULT);
			asDefault().injectingInto(EventProcessor.class).bind(
					ExecutorService.class).toProvider(Executors::newWorkStealingPool);
		}

	}

}
