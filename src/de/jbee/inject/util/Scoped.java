package de.jbee.inject.util;

import java.util.HashMap;
import java.util.Map;

import de.jbee.inject.Demand;
import de.jbee.inject.Dependency;
import de.jbee.inject.Injectable;
import de.jbee.inject.Repository;
import de.jbee.inject.Scope;

public class Scoped { //OPEN what about Scoping ?

	interface KeyDeduction {

		<T> String deduceKey( Demand<T> demand );
	}

	public static final KeyDeduction DEPENDENCY_TYPE_KEY = new DependencyTypeAsKey();
	public static final KeyDeduction TARGET_INSTANCE_KEY = new TargetInstanceAsKey();

	/**
	 * Often called the 'default' or 'prototype'-scope. Asks the {@link Injectable} once per
	 * injection.
	 */
	public static final Scope INJECTION = new InjectionScope();
	/**
	 * Asks the {@link Injectable} once per binding. Thereby instances become singletons local to
	 * the application.
	 */
	public static final Scope APPLICATION = new ApplicationScope();
	/**
	 * Asks the {@link Injectable} once per thread per binding which is understand commonly as a
	 * usual 'per-thread' singleton.
	 */
	public static final Scope THREAD = new ThreadScope( new ThreadLocal<Repository>(), APPLICATION );

	public static final Scope DEPENDENCY_TYPE = uniqueBy( DEPENDENCY_TYPE_KEY );
	public static final Scope TARGET_INSTANCE = uniqueBy( TARGET_INSTANCE_KEY );

	public static Scope uniqueBy( KeyDeduction keyDeduction ) {
		return new KeyDeductionScope( keyDeduction );
	}

	public static Repository asSnapshot( Repository src, Repository dest ) {
		return new SnapshotRepository( src, dest );
	}

	/**
	 * What is usually called a 'default'-{@link Scope} will ask the {@link Injectable} passed each
	 * time the {@link Repository#serve(Dependency, Injectable)}-method is invoked.
	 * 
	 * The {@link Scope} is also used as {@link Repository} instance since both don#t have any
	 * state.
	 * 
	 * @see Scoped#INJECTION
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	private static final class InjectionScope
			implements Scope, Repository {

		InjectionScope() {
			// make visible
		}

		@Override
		public Repository init() {
			return this;
		}

		@Override
		public <T> T serve( Demand<T> demand, Injectable<T> injectable ) {
			return injectable.instanceFor( demand );
		}

		@Override
		public String toString() {
			return "(default)";
		}

	}

	private static final class ThreadScope
			implements Scope, Repository {

		private final ThreadLocal<Repository> threadRepository;
		private final Scope repositoryScope;

		ThreadScope( ThreadLocal<Repository> threadRepository, Scope repositoryScope ) {
			super();
			this.threadRepository = threadRepository;
			this.repositoryScope = repositoryScope;
		}

		@Override
		public <T> T serve( Demand<T> demand, Injectable<T> injectable ) {
			Repository repository = threadRepository.get();
			if ( repository == null ) {
				// since each thread is just accessing its own repo there cannot be a repo set for the running thread after we checked for null
				repository = repositoryScope.init();
				threadRepository.set( repository );
			}
			return repository.serve( demand, injectable );
		}

		@Override
		public Repository init() {
			return this;
		}

		@Override
		public String toString() {
			return "(per-thread)";
		}
	}

	/**
	 * The 'synchronous'-{@link Repository} will be asked first passing a special resolver that will
	 * ask the 'asynchronous' repository when invoked. Thereby the repository originally bound will
	 * be asked once. Thereafter the result is stored in the synchronous repository.
	 * 
	 * Both repositories will remember the resolved instance whereby the repository considered as
	 * the synchronous-repository will deliver a consistent image of the world as long as it exists.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	private static final class SnapshotRepository
			implements Repository {

		private final Repository dest;
		private final Repository src;

		SnapshotRepository( Repository src, Repository dest ) {
			super();
			this.dest = dest;
			this.src = src;
		}

		@Override
		public <T> T serve( Demand<T> demand, Injectable<T> injectable ) {
			//FIXME at some point the dest repo is outdated - do we ask the src again in that case ? I'm note sure 
			return dest.serve( demand, new SnapshotingInjectable<T>( injectable, src ) );
		}

		private static final class SnapshotingInjectable<T>
				implements Injectable<T> {

			private final Injectable<T> supplier;
			private final Repository src;

			SnapshotingInjectable( Injectable<T> supplier, Repository src ) {
				super();
				this.supplier = supplier;
				this.src = src;
			}

			@Override
			public T instanceFor( Demand<T> demand ) {
				return src.serve( demand, supplier );
			}

		}

	}

	private static final class KeyDeductionScope
			implements Scope {

		private final KeyDeduction keyDeduction;

		KeyDeductionScope( KeyDeduction keyDeduction ) {
			super();
			this.keyDeduction = keyDeduction;
		}

		@Override
		public Repository init() {
			return new KeyDeductionRepository( keyDeduction );
		}

		@Override
		public String toString() {
			return "(per-" + keyDeduction + ")";
		}

	}

	private static final class TargetInstanceAsKey
			implements KeyDeduction {

		TargetInstanceAsKey() {
			// make visible
		}

		@Override
		public <T> String deduceKey( Demand<T> injection ) {
			return injection.dependency().target( 1 ).toString();
		}

		@Override
		public String toString() {
			return "target-instance";
		}

	}

	private static final class DependencyTypeAsKey
			implements KeyDeduction {

		DependencyTypeAsKey() {
			// make visible
		}

		@Override
		public <T> String deduceKey( Demand<T> demand ) {
			return demand.dependency().getType().toString();
		}

		@Override
		public String toString() {
			return "dependendy-type";
		}

	}

	// e.g. get receiver class from dependency -to be reusable the provider could offer a identity --> a wrapper class would be needed anyway so maybe best is to have quite similar impl. all using a identity hash-map

	private static final class KeyDeductionRepository
			implements Repository {

		private final Map<String, Object> instances = new HashMap<String, Object>();
		private final KeyDeduction injectionKey;

		KeyDeductionRepository( KeyDeduction injectionKey ) {
			super();
			this.injectionKey = injectionKey;
		}

		@Override
		@SuppressWarnings ( "unchecked" )
		public <T> T serve( Demand<T> demand, Injectable<T> injectable ) {
			final String key = injectionKey.deduceKey( demand );
			T instance = (T) instances.get( key );
			if ( instance != null ) {
				return instance;
			}
			synchronized ( instances ) {
				instance = (T) instances.get( key );
				if ( instance == null ) {
					instance = injectable.instanceFor( demand );
					instances.put( key, instance );
				}
			}
			return instance;
		}

	}

	/**
	 * Will lead to instances that can be seen as application-wide-singletons.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 * 
	 */
	private static final class ApplicationScope
			implements Scope {

		ApplicationScope() {
			//make visible
		}

		@Override
		public Repository init() {
			return new ResourceRepository();
		}

		@Override
		public String toString() {
			return "(per-app)";
		}
	}

	/**
	 * Contains once instance per resource. Resources are never updated. This can be used to create
	 * a thread or request {@link Scope}.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	private static final class ResourceRepository
			implements Repository {

		private Object[] instances;

		ResourceRepository() {
			super();
		}

		@Override
		@SuppressWarnings ( "unchecked" )
		public <T> T serve( Demand<T> demand, Injectable<T> injectable ) {
			if ( instances == null ) {
				instances = new Object[demand.envCardinality()];
			}
			T res = (T) instances[demand.envSerialNumber()];
			if ( res != null ) {
				return res;
			}
			// just sync the (later) unexpected path that is executed once
			synchronized ( instances ) {
				res = (T) instances[demand.envSerialNumber()];
				if ( res == null ) { // we need to ask again since the instance could have been initialized before we got entrance to the sync block
					res = injectable.instanceFor( demand );
					instances[demand.envSerialNumber()] = res;
				}
			}
			return res;
		}

	}

}
