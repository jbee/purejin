package se.jbee.inject.event;

import se.jbee.inject.binder.BinderModule;

public abstract class EventModule extends BinderModule {

	protected EventModule() {
		super(EventBaseModule.class);
	}

	/**
	 * Responsible for setting up the basics of event feature.
	 */
	private static final class EventBaseModule extends BinderModule {

		@Override
		protected void declare() {

		}
	}
}
