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
	 * {@link EventProcessor#proxy(Class)} is injected.
	 * 
	 * @param event the type of the event/listener (must be an interface)
	 */
	protected <T> void handle(Class<T> event) {
		if (!event.isInterface())
			throw new IllegalArgumentException("Event type has to be an interface but was: " + event);
		initbind(event).to((Initialiser<T>) (listener, injector) ->
			injector.resolve(EventProcessor.class).register(event, listener));
		bind(event).to((Supplier<T>)(dep, injector) ->
			injector.resolve(EventProcessor.class).proxy(event));
	}
	
	private static final class EventBaseModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault().bind(EventProcessor.class).to(AsyncEventProcessor.class);
		}
		
	}
	
	
	// events or event listers are interfaces
	// these are somehow marked as events => better bound as this allows "marking" existing and external interfaces
	// what the module does is automatically linking published events to the listeners
	// there can be synchronous events and asynchronous events
	
	// plugin(MyEventListener.class).into(Event.class);
	
	// API
	// user binds listener interfaces
	// dynamic init picks up all implementers and registers them to the event processing unit
	// user asks for "the" interface implementation (default or special) which is a proxy tp the event processing unit
	// when user invokes the listener method in the proxy this creates a message: Class of the interface, String methodName, Object[] args
	// each message is processed by the processing unit which knows all the actual listeners and uses reflection to invoke their listener method from the message data
	// e.g. annotations on the listener fields can be used to receive a proxy with non standard properties. E.g. async/sync, groups of receivers and such things
	
	// isolation:
	// easy to process events so that the handler method is always just called by one thread at a time and hence does not have to support multi-threading
}
