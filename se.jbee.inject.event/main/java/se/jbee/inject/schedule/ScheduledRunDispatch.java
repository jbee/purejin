package se.jbee.inject.schedule;

import se.jbee.inject.event.*;

import java.util.Map;

public class ScheduledRunDispatch
		implements EventDispatch<Schedule.Run> {

	@Override
	public void dispatch(Event<Schedule.Run> event,
			Map<EventTarget, EventHandler<? super Schedule.Run>> handlers,
			EventDispatcher dispatcher) {
		EventHandler<? super Schedule.Run> receiver = handlers.get(event.type.target);
		if (receiver == null) {
			//TODO
			return;
		}
		dispatcher.handle(event, receiver);
	}

}
