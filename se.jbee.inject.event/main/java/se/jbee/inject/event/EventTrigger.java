package se.jbee.inject.event;

import java.util.function.Consumer;

@FunctionalInterface
public interface EventTrigger<T> {

	void register(Consumer<? extends T> listener);
}
