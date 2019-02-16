package se.jbee.inject.event;

import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.container.Initialiser;
import se.jbee.inject.container.Scoped;
import se.jbee.inject.container.Supplier;

public abstract class EventModule extends BinderModule {

	protected EventModule() {
		super(Scoped.APPLICATION, EventBaseModule.class);
	}

	/**
	 * Registers the given event type so that it is handled by the
	 * {@link EventProcessor} framework.
	 * 
	 * That means classes implementing the given event interface automatically
	 * receive calls to any of the interface methods. When The event interface
	 * should be injected to signal/call one of its methods a
	 * {@link EventProcessor#getProxy(Class)} is injected.
	 * 
	 * @param event the type of the event/listener (must be an interface)
	 */
	protected <T> void handle(Class<T> event) {
		if (!event.isInterface())
			throw new IllegalArgumentException("Event type has to be an interface but was: " + event);
		initbind(event).to((Initialiser<T>) (listener, injector) ->
			injector.resolve(EventProcessor.class).register(event, listener));
		bind(event).to((Supplier<T>)(dep, injector) ->
			injector.resolve(EventProcessor.class).getProxy(event));
	}
	
	private static final class EventBaseModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault().bind(EventProcessor.class).to(ConcurrentEventProcessor.class);
			asDefault().bind(EventReflector.class).to(event -> EventProperties.DEFAULT);
		}
		
	}

}
