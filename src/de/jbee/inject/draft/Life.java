/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject.draft;

import de.jbee.inject.Dependency;

public class Life {

	public static final Lifecycle ETERNAL = new EternalLifecycle();
	public static final Lifecycle SINGULAR = new SingularLifecycle();

	public static final Lifespan<Thread> CURRENT_THREAD = new CurrentThreadLifespan();

	public static final Lifespan<Object> JVM = new ObjectLifespan<Object>( Object.class );

	private static final class ObjectLifespan<T>
			implements Lifespan<T> {

		private final T origination;

		ObjectLifespan( T origination ) {
			super();
			this.origination = origination;
		}

		@Override
		public Lifecycle cycle( T origination ) {
			return ETERNAL; // OPEN make this a field ? 
		}

		@Override
		public T origination( Dependency<?> dependency ) {
			return origination;
		}

	}

	private static final class ThreadLifespan
			implements Lifespan<Thread> {

		private final Thread origination;

		ThreadLifespan( Thread origination ) {
			super();
			this.origination = origination;
		}

		@Override
		public Lifecycle cycle( Thread origination ) {
			return new ThreadLifecycle( origination );
		}

		@Override
		public Thread origination( Dependency<?> dependency ) {
			return origination;
		}
	}

	private static final class EternalLifecycle
			implements Lifecycle {

		EternalLifecycle() {
			// make visible
		}

		@Override
		public boolean isContinuing() {
			return true;
		}

	}

	private static final class SingularLifecycle
			implements Lifecycle {

		SingularLifecycle() {
			// make visible
		}

		@Override
		public boolean isContinuing() {
			return false;
		}

	}

	private static final class CurrentThreadLifespan
			implements Lifespan<Thread> {

		CurrentThreadLifespan() {
			// make visible
		}

		@Override
		public Lifecycle cycle( Thread origination ) {
			return new ThreadLifecycle( origination );
		}

		@Override
		public Thread origination( Dependency<?> dependency ) {
			return Thread.currentThread();
		}
	}

	private static final class ThreadLifecycle
			implements Lifecycle {

		private Thread origination;

		ThreadLifecycle( Thread origination ) {
			super();
			this.origination = origination;
		}

		@Override
		public boolean isContinuing() {
			if ( origination == null ) {
				return true;
			}
			boolean alive = origination.isAlive();
			if ( !alive ) {
				origination = null; // make GC possible
			}
			return alive;
		}

	}
}
