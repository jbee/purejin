/**
 * Contains components of a basic add-on to do scheduled {@link
 * java.lang.reflect.Method} calls.
 * <p>
 * The methods to call are identified using {@connect} feature which is based on
 * {@link se.jbee.inject.Lift}s.
 * <p>
 * The {@link se.jbee.inject.schedule.Scheduled} annotation can be used to mark
 * methods that should be called and define their {@link
 * se.jbee.inject.schedule.Schedule}.
 * <p>
 * Annotated methods can have parameters which are injected. The idea is to
 * reverse the code dependencies. Instead of making all classes dependent on the
 * code that runs the scheduling the desired schedule is declared in a
 * declarative way. This allows to switch the executing code without modifying
 * any of the classes that have the need for scheduled tasks. Also all
 * dependencies that are only needed to run the task body can be made parameters
 * of the scheduled method reducing the need for fields that are otherwise not
 * used in the class.
 * <p>
 * Users can add custom ways to derive a {@link se.jbee.inject.schedule.Schedule}
 * by registering a named {@link se.jbee.inject.schedule.Schedule.ScheduleFactory}.
 */
package se.jbee.inject.schedule;
