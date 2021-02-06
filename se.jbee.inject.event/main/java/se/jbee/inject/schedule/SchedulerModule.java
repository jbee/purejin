package se.jbee.inject.schedule;

import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.config.Config;
import se.jbee.inject.config.Connector;
import se.jbee.inject.config.HintsBy;
import se.jbee.inject.config.Invoke;
import se.jbee.inject.lang.Type;
import se.jbee.inject.schedule.Schedule.ScheduleFactory;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.binder.spi.ConnectorBinder.SCHEDULER_CONNECTOR;
import static se.jbee.inject.lang.Cast.consumerTypeOf;
import static se.jbee.inject.lang.Type.*;

public final class SchedulerModule extends BinderModule {

	public static final Type<Consumer<Schedule>> SCHEDULER_TYPE = consumerTypeOf(Schedule.class);

	/**
	 * Main point of this interface is to limit the surface of the API to set in
	 * oder to change the execution of the {@link DefaultScheduler}.
	 * <p>
	 * This does correspond to {@link java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(Runnable,
	 * long, long, TimeUnit)}.
	 */
	@FunctionalInterface
	public interface ScheduledExecutor {

		void executeInSchedule(Runnable task, long initialDelay, long period, TimeUnit unit);
	}

	@Override
	protected void declare() {
		asDefault().bind(Name.ANY.in(SCHEDULER_CONNECTOR), raw(Connector.class)) //
				.toSupplier(SchedulerConnector.class);
		asDefault().bind(SCHEDULER_TYPE) //
				.to(DefaultScheduler.class);
		asDefault().bind(ScheduledExecutor.class) //
				.toProvider(() -> Executors.newSingleThreadScheduledExecutor()::scheduleAtFixedRate);
		asDefault().bind(named(Scheduled.class), ScheduleFactory.class)
				.to(SchedulerModule::annotated);

		// connect Scheduled
		schedule(Scheduled.Aware.class, Scheduled.class);
	}

	public static Schedule annotated(Object obj, Type<?> as, Method target, Injector context) {
		Scheduled scheduled = target.getAnnotation(Scheduled.class);
		long intervalMillis = scheduled.unit().toMillis(scheduled.every());
		String property = scheduled.by();
		if (!property.isEmpty()) {
			intervalMillis = context.resolve(dependency(Config.class).injectingInto(as))
					.longValue(property, intervalMillis);
		}
		return new Schedule(obj, as, target, Duration.ofMillis(intervalMillis),
				LocalDateTime.now());
	}

	public static class DefaultScheduler implements Consumer<Schedule> {

		private final Injector context;
		private final HintsBy hintsBy;
		private final ScheduledExecutor executor;

		public DefaultScheduler(Injector context, ScheduledExecutor executor) {
			this.context = context;
			this.executor = executor;
			this.hintsBy = context.resolve(Env.class)
					.in(DefaultScheduler.class)
					.property(HintsBy.class);
		}

		@Override
		public void accept(Schedule schedule) {
			executor.executeInSchedule(createTask(schedule),
					schedule.delayNow().toMillis(),
					schedule.interval.toMillis(),
					TimeUnit.MILLISECONDS);
		}

		private Runnable createTask(Schedule schedule) {
			Method target = schedule.scheduled;
			Type<?> objType = actualInstanceType(schedule.instance, schedule.as);
			Dependency<?> dep = dependency(
					actualReturnType(target, objType)) //
					.injectingInto(objType);
			InjectionSite site = new InjectionSite(context, dep,
					hintsBy.applyTo(context, target, objType));
			Invoke invoke = context.resolve(dependency(Invoke.class) //
					.injectingInto(target.getDeclaringClass()));
			return () -> {
				try {
					invoke.call(target, schedule.instance, site.args(context));
				} catch (Exception ex) {
					//TODO log or event?
				}
			};
		}
	}

	public static class SchedulerConnector implements Supplier<Connector> {

		private final Injector context;
		private final Consumer<Schedule> scheduler;
		private final Map<String, ScheduleFactory> resolverByName = new HashMap<>();

		public SchedulerConnector(Injector context, Consumer<Schedule> scheduler) {
			this.context = context;
			this.scheduler = scheduler;
		}

		@Override
		public Connector supply(Dependency<? super Connector> dep,
				Injector context) throws UnresolvableDependency {
			String type = dep.instance.name.withoutNamespace();
			return ((instance, as, connected) -> connect(instance, as, connected, type));
		}

		private void connect(Object instance, Type<?> as, Method connected, String type) {
			ScheduleFactory factory = resolverByName.computeIfAbsent(type, //
							name -> context.resolve(name, ScheduleFactory.class));
			if (factory == null)
				throw new InconsistentDeclaration(
						"Scheduler factory type not bound: " + type);
			scheduler.accept(factory.create(instance, as, connected, context));
		}
	}
}
