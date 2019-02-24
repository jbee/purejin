/*
 *  Copyright (c) 2012-2017, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import static se.jbee.inject.Scoping.scopingOf;

import java.util.HashMap;
import java.util.Map;

import se.jbee.inject.Dependency;
import se.jbee.inject.Generator;
import se.jbee.inject.Provider;
import se.jbee.inject.Repository;
import se.jbee.inject.Scope;
import se.jbee.inject.InjectionCase;
import se.jbee.inject.UnresolvableDependency;

/**
 * Utility as a factory to create/use {@link Scope}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Scoped {

	/**
	 * Often called the 'default' or 'prototype'-scope. Asks the {@link Provider} once per
	 * injection.
	 */
	public static final Scope INJECTION = new InjectionScope();
	/**
	 * Asks the {@link Provider} once per binding. Thereby instances become singletons local to
	 * the application.
	 */
	public static final Scope APPLICATION = new ApplicationScope();
	/**
	 * Asks the {@link Provider} once per thread per binding which is understand commonly as a
	 * usual 'per-thread' singleton.
	 */
	public static final Scope THREAD = new ThreadScope(new ThreadLocal<>(), APPLICATION );

	public static final Scope DEPENDENCY_TYPE = new DependencyTypeScope();
	public static final Scope DEPENDENCY_INSTANCE = new DependencyInstanceScope();
	public static final Scope TARGET_INSTANCE = new TargetInstanceScope();
	public static final Scope DEPENDENCY = new TargetedDependencyTypeScope();

	public static Repository asSnapshot( Repository src, Repository dest ) {
		return new SnapshotRepository( src, dest );
	}
	
	static {
		scopingOf(INJECTION)
			.notStableIn(THREAD)
			.notStableIn(APPLICATION)
			.notStableIn(DEPENDENCY)
			.notStableIn(DEPENDENCY_INSTANCE)
			.notStableIn(DEPENDENCY_TYPE)
			.notStableIn(TARGET_INSTANCE);
		scopingOf(THREAD)
			.notStableIn(APPLICATION)
			.notStableIn(DEPENDENCY)
			.notStableIn(DEPENDENCY_INSTANCE)
			.notStableIn(DEPENDENCY_TYPE)
			.notStableIn(TARGET_INSTANCE);
	}

	/**
	 * What is usually called a 'default'-{@link Scope} will ask the
	 * {@link Provider} passed each time the
	 * {@link Repository#serve(Dependency, InjectionCase, Provider)}}-method is
	 * invoked.
	 *
	 * The {@link Scope} is also used as {@link Repository} instance since both
	 * don#t have any state.
	 *
	 * @see Scoped#INJECTION
	 */
	private static final class InjectionScope implements Scope, Repository {

		InjectionScope() { /* make visible */ }
		
		@Override
		public Repository init(int generators) {
			return this;
		}

		@Override
		public <T> T serve(int serialID, Dependency<? super T> dep, Provider<T> provider) 
				throws UnresolvableDependency {
			return provider.provide();
		}

		@Override
		public String toString() {
			return "(default)";
		}

	}

	private static final class ThreadScope implements Scope, Repository {

		private final ThreadLocal<Repository> threadRepository;
		private final Scope repositoryScope;
		private int generators;

		ThreadScope( ThreadLocal<Repository> threadRepository, Scope repositoryScope ) {
			this.threadRepository = threadRepository;
			this.repositoryScope = repositoryScope;
		}

		@Override
		public <T> T serve(int serialID, Dependency<? super T> dep, Provider<T> provider)
				throws UnresolvableDependency {
			Repository repository = threadRepository.get();
			if ( repository == null ) {
				// since each thread is just accessing its own repo there cannot be a repo set for the running thread after we checked for null
				repository = repositoryScope.init(generators);
				threadRepository.set( repository );
			}
			return repository.serve( serialID, dep, provider );
		}

		@Override
		public Repository init(int generators) {
			this.generators = generators;
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
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	private static final class SnapshotRepository implements Repository {

		private final Repository src;
		private final Repository dest;

		SnapshotRepository( Repository src, Repository dest ) {
			this.src = src;
			this.dest = dest;
		}

		@Override
		public <T> T serve(int serialID, Dependency<? super T> dep, Provider<T> provider)
				throws UnresolvableDependency {
			return dest.serve(serialID, dep, new SnapshotingProvider<>(dep, serialID, provider, src));
		}

		private static final class SnapshotingProvider<T> implements Provider<T> {

			private final Dependency<? super T> dependency;
			private final int serialID;
			private final Provider<T> supplier;
			private final Repository src;

			SnapshotingProvider(Dependency<? super T> dep, int serialID, Provider<T> supplier, Repository src) {
				this.dependency = dep;
				this.serialID = serialID;
				this.supplier = supplier;
				this.src = src;
			}

			@Override
			public T provide() {
				return src.serve(serialID, dependency, supplier);
			}
		}

	}

	public static abstract class DependencyBasedScope implements Scope.SingletonScope {

		private final String property;

		DependencyBasedScope( String property ) {
			this.property = property;
		}

		@Override
		public Repository init(int generators) {
			return new DependencyBasedRepository( this );
		}
		
		abstract <T> String instanceKeyFor( Dependency<T> dep );

		@Override
		public String toString() {
			return "(per-" + property + ")";
		}

	}

	private static final class TargetedDependencyTypeScope extends DependencyBasedScope {

		TargetedDependencyTypeScope() {
			super("targeted-dependency-type");
		}
		@Override
		public <T> String instanceKeyFor( Dependency<T> dep ) {
			return instanceNameOf(dep) + targetInstanceOf(dep);
		}

	}

	private static final class TargetInstanceScope extends DependencyBasedScope {
		TargetInstanceScope() {
			super("target-instance");
		}

		@Override
		public <T> String instanceKeyFor( Dependency<T> dep ) {
			return targetInstanceOf(dep);
		}

	}
	
	public static <T> String targetInstanceOf(Dependency<T> dep) {
		StringBuilder b = new StringBuilder();
		for ( int i = dep.injectionDepth() - 1; i >= 0; i-- ) {
			b.append( dep.target( i ) );
		}
		return b.toString();
	}

	private static final class DependencyTypeScope extends DependencyBasedScope {
		DependencyTypeScope() {
			super("dependendy-type");
		}

		@Override
		public <T> String instanceKeyFor( Dependency<T> dep ) {
			return dep.type().toString();
		}
	}

	private static final class DependencyInstanceScope extends DependencyBasedScope {
		DependencyInstanceScope() {
			super("dependendy-instance");
		}

		@Override
		public <T> String instanceKeyFor( Dependency<T> dep ) {
			return instanceNameOf(dep);
		}
	}

	public static <T> String instanceNameOf(Dependency<T> dep) {
		return dep.instance.name.toString() + "@" + dep.type().toString();
	}

	// e.g. get receiver class from dependency -to be reusable the provider could offer a identity --> a wrapper class would be needed anyway so maybe best is to have quite similar impl. all using a identity hash-map

	private static final class DependencyBasedRepository implements Repository {

		private final Map<String, Object> instances = new HashMap<>();
		private final DependencyBasedScope scope;

		DependencyBasedRepository( DependencyBasedScope scope ) {
			this.scope = scope;
		}

		@Override
		@SuppressWarnings ( "unchecked" )
		public <T> T serve(int serialID, Dependency<? super T> dep, Provider<T> provider)
				throws UnresolvableDependency {
			final String key = scope.instanceKeyFor( dep );
			T instance = (T) instances.get( key );
			if ( instance != null ) {
				return instance;
			}
			synchronized ( instances ) {
				instance = (T) instances.get( key );
				if ( instance == null ) {
					instance = provider.provide();
					instances.put( key, instance );
				}
			}
			return instance;
		}

	}

	/**
	 * Will lead to instances that can be seen as application-wide-singletons.
	 */
	private static final class ApplicationScope implements Scope.SingletonScope {

		ApplicationScope() { /* make visible */ }

		@Override
		public Repository init(int generators) {
			return new LazySerialRepository(generators);
		}

		@Override
		public String toString() {
			return "(per-app)";
		}
	}

	/**
	 * Contains once instance per {@link Generator}. Instances are never
	 * updated. This can be used to create a thread, request or application
	 * {@link Scope}.
	 */
	private static final class LazySerialRepository implements Repository {

		private final int generators;
		private Object[] instances;

		LazySerialRepository(int generators) {
			this.generators = generators;
		}

		@Override
		@SuppressWarnings ( "unchecked" )
		public <T> T serve(int serialID, Dependency<? super T> dep, Provider<T> provider)
				throws UnresolvableDependency {
			if ( instances == null ) {
				instances = new Object[generators];
			}
			T res = (T) instances[serialID];
			if ( res != null ) {
				return res;
			}
			// just sync the (later) unexpected path that is executed once
			synchronized ( instances ) {
				res = (T) instances[serialID];
				if ( res == null ) { // we need to ask again since the instance could have been initialized before we got entrance to the sync block
					res = provider.provide();
					instances[serialID] = res;
				}
			}
			return res;
		}

	}

}
