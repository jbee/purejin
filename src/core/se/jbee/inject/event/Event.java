package se.jbee.inject.event;

public class Event {

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
}
