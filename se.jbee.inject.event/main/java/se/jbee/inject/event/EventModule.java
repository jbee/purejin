package se.jbee.inject.event;

import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.spi.ConnectorBinder;
import se.jbee.inject.config.Connector;

import java.util.function.Consumer;

import static se.jbee.inject.event.EventTrigger.eventTriggerTypeOf;

public abstract class EventModule extends BinderModule {

	protected EventModule() {
		super(EventBaseModule.class);
	}

	/**
	 * Responsible for setting up the basics of event feature.
	 */
	public static final class EventBaseModule extends BinderModule {

		@Override
		protected void declare() {
			// have the dispatcher receive "event" type method connections
			asDefault().bind(ConnectorBinder.EVENT_CONNECTOR, Connector.class)
					.to(DefaultEventDispatcher.class);

			// connect On
			receiveIn(On.Aware.class, On.class);
			//TODO should read: in(On.Aware.class).receive(On.class);
			// or: receive(On.class).in(On.Aware.class);

			// hook Shutdown into Runtime
			asDefault().bind(eventTriggerTypeOf(On.Shutdown.class)) //
					.to(EventBaseModule::activateOnShutdown);
		}

		private static void activateOnShutdown(Consumer<On.Shutdown> dispatcher) {
			Runtime.getRuntime().addShutdownHook(new Thread(
					() -> dispatcher.accept(new On.Shutdown())));
		}
	}

	// use events to process schedules
	// scheduler is just an event trigger
	// add Run class to Schedule which is the event to run a scheduled method
	// when a schedule is connected we also connect it as an event
	// all events use a special dispatcher: ScheduledDispatch which uses a target ID to pick the richtig EventTarget/EventSite/Receiver by this ID (full method name)
	// this means scheduled methods can also return events they want to trigger
}
