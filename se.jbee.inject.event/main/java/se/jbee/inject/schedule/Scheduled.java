package se.jbee.inject.schedule;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface Scheduled {

	/**
	 * Marker interface to be implemented by managed instances that use the
	 * {@link Scheduled} annotation to mark methods that should be scheduled.
	 * <p>
	 * There is nothing special about this interface except that the {@link
	 * SchedulerModule} connects it to the {@link Scheduled} annotation so
	 * instances of implementing classes get connected via {@link
	 * se.jbee.inject.Lift}.
	 */
	interface Aware {}

	/**
	 * @return The {@link TimeUnit} used for the {@link #every()} and {@link #starting()} property.
	 */
	TimeUnit unit() default TimeUnit.SECONDS;

	/**
	 * @return length of the scheduled interval in millis, seconds, minutes (depending on {@link #unit()})
	 */
	int every() default 1;

	/**
	 * @return
	 */
	int starting() default -1;

	/**
	 * @return name of the {@link se.jbee.inject.config.Config} property in case
	 * the time is not given directly by {@link #every()} and {@link #unit()}.
	 * <p>
	 * The annotated type is used as context for the {@link
	 * se.jbee.inject.config.Config}.
	 * <p>
	 * If this property is defined the time defined by {@link #every()} and
	 * {@link #unit()} acts as a default or fallback in case the configuration
	 * is not defined.
	 */
	String by() default "";

	/**
	 * @return maximum number of consecutive execution failures after which
	 * scheduling is cancelled. If zero or negative execution is never
	 * cancelled.
	 */
	int maxFails() default 1;
}
