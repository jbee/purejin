package se.jbee.inject.scope;

import se.jbee.inject.Dependency;
import se.jbee.inject.Provider;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * A {@link Scope} that is linked to the current {@link Thread} using the
 * {@link Controller}.
 */
public final class WorkerScope implements Scope {

	//TODO cleanup that checks if thread is alive => general feature to allow hook in for cleanup without needed to explicitly work with scheduler
	// also to cleanup on JVM shutdown

	private static final class WorkerState {
		final AtomicReferenceArray<Object> instances;

		WorkerState(AtomicReferenceArray<Object> instances) {
			this.instances = instances;
		}
	}

	private final ConcurrentMap<Thread, WorkerState> states = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	@Override
	public <T> T provide(int serialID, int resources, Dependency<? super T> dep,
			Provider<T> provider) throws UnresolvableDependency {
		Thread target = Thread.currentThread();
		if (dep.type().rawType == Controller.class) {
			return (T) new WorkerScopeController(resources, target, states);
		}
		WorkerState state = states.get(target);
		if (state == null) {
			throw new UnresolvableDependency.SupplyFailed("Scope error",
					contextNotAllocated("Context"));
		}
		return (T) state.instances.updateAndGet(serialID,
				value -> value != null ? value : provider.provide());
	}

	static IllegalStateException contextNotAllocated(String context) {
		return new IllegalStateException(context + " was not allocated using "
			+ Controller.class.getSimpleName());
	}

	private static final class WorkerScopeController implements Controller {

		final int generators;
		final Thread src;
		final ConcurrentMap<Thread, WorkerState> workerStates;
		private volatile WorkerState srcWorkerState;

		WorkerScopeController(int generators, Thread src,
				ConcurrentMap<Thread, WorkerState> states) {
			this.generators = generators;
			this.src = src;
			this.workerStates = states;
			this.srcWorkerState = states.get(src);
		}

		@Override
		public void allocate() {
			Thread target = Thread.currentThread();
			WorkerState state = getOrCreateState(target);
			WorkerState before = workerStates.putIfAbsent(target, state);
			if (before != null) {
				throw new IllegalStateException("Context was not deallocated.");
			}
		}

		private WorkerState getOrCreateState(Thread target) {
			if (src == target) {
				srcWorkerState = new WorkerState(
						new AtomicReferenceArray<>(generators));
				return srcWorkerState;
			}
			WorkerState state = srcWorkerState;
			if (state == null)
				throw contextNotAllocated("Transfer context");
			return new WorkerState(state.instances);
		}

		@Override
		public void deallocate() {
			workerStates.remove(Thread.currentThread());
		}

	}
}
