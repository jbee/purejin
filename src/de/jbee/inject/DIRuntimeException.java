package de.jbee.inject;

public class DIRuntimeException
		extends RuntimeException {

	public DIRuntimeException( String message ) {
		super( message );
	}

	@Override
	public String toString() {
		return getMessage();
	}

	public static final class DependencyCycleException
			extends DIRuntimeException {

		private final Dependency<?> dependency;
		private final Instance<?> cycleTarget;

		public DependencyCycleException( Dependency<?> dependency, Instance<?> cycleTarget ) {
			super( cycleTarget + " into " + dependency );
			this.dependency = dependency;
			this.cycleTarget = cycleTarget;
		}
	}

	public static final class MoreFrequentExpiryException
			extends DIRuntimeException {

		public MoreFrequentExpiryException( Injection parent, Injection injection ) {
			super( "Cannot inject " + injection.getTarget() + " into " + parent.getTarget() );
		}

	}

	/**
	 * An {@link Injector} couldn't find a {@link Resource} that matches a {@link Dependency} to
	 * resolve.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 * 
	 */
	public static final class NoSuchResourceException
			extends DIRuntimeException {

		private final Dependency<?> dependency;

		public <T> NoSuchResourceException( Dependency<T> dependency, Injectron<T>[] available ) {
			super( "No resource found that matches dependency: " + dependency );
			this.dependency = dependency;
		}

	}
}
