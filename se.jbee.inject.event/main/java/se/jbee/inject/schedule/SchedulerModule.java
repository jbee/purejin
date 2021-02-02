package se.jbee.inject.schedule;

import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.config.Connector;
import se.jbee.inject.config.HintsBy;
import se.jbee.inject.config.Invoke;
import se.jbee.inject.schedule.Schedule.ScheduleFactory;
import se.jbee.inject.lang.Type;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.binder.spi.ConnectorBinder.SCHEDULER_CONNECTOR;
import static se.jbee.inject.lang.Cast.consumerTypeOf;
import static se.jbee.inject.lang.Type.actualReturnType;
import static se.jbee.inject.lang.Type.raw;

public final class SchedulerModule extends BinderModule {

	public static final Type<Consumer<Schedule>> SCHEDULER_TYPE = consumerTypeOf(Schedule.class);

	@Override
	protected void declare() {
		asDefault().bind(Name.ANY.in(SCHEDULER_CONNECTOR), raw(Connector.class)) //
				.toSupplier(SchedulerConnector.class);
		asDefault().bind(SCHEDULER_TYPE) //
				.to(DefaultScheduler.class);
		asDefault().injectingInto(DefaultScheduler.class) //
				.bind(ScheduledExecutorService.class) //
				.toProvider(Executors::newSingleThreadScheduledExecutor);
		asDefault().bind(named(Scheduled.class), ScheduleFactory.class)
				.to(SchedulerModule::annotated);
	}

	public static Schedule annotated(Object obj, Type<?> as, Method target) {
		Scheduled scheduled = target.getAnnotation(Scheduled.class);
		long intervalMillis = scheduled.unit().toMillis(scheduled.every());
		return new Schedule(obj, as, target, intervalMillis, 0L);
	}

	public static class DefaultScheduler implements Consumer<Schedule> {

		private final Injector context;
		private final HintsBy hintsBy;
		private final ScheduledExecutorService executor;

		public DefaultScheduler(Injector context, ScheduledExecutorService executor) {
			this.context = context;
			this.executor = executor;
			this.hintsBy = context.resolve(Env.class)
					.in(DefaultScheduler.class)
					.property(HintsBy.class);
		}

		@Override
		public void accept(Schedule schedule) {
			executor.scheduleAtFixedRate(createTask(schedule), schedule.initialDelayMillis,
					schedule.intervalMillis,
					TimeUnit.MILLISECONDS);
		}

		private Runnable createTask(Schedule schedule) {
			Method target = schedule.scheduled;
			Dependency<?> dep = dependency(
					actualReturnType(target, schedule.as)) //
					.injectingInto(schedule.as);
			InjectionSite site = new InjectionSite(context, dep,
					hintsBy.applyTo(context, target, schedule.as));
			Invoke invoke = context.resolve(dependency(Invoke.class) //
					.injectingInto(target.getDeclaringClass()));
			return () -> {
				try {
					invoke.call(target, schedule.instance, site.args(context));
				} catch (Exception ex) {
					//TODO log
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
			scheduler.accept(factory.create(instance, as, connected));
		}
	}
}
