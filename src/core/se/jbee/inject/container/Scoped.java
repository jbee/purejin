/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import java.util.HashMap;
import java.util.Map;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injectron;
import se.jbee.inject.InjectronInfo;

/**
 * Utility as a factory to create/use {@link Scope}s.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Scoped {

	public interface DependencyProperty {

		<T> String deriveFrom( Dependency<T> dependency );
	}

	public static final DependencyProperty DEPENDENCY_TYPE_KEY = new DependencyTypeProperty();
	public static final DependencyProperty DEPENDENCY_INSTANCE_KEY = new DependencyInstanceProperty();
	public static final DependencyProperty TARGET_INSTANCE_KEY = new TargetInstanceProperty();
	public static final DependencyProperty TARGETED_DEPENDENCY_TYPE_KEY = new CombinedProperty(DEPENDENCY_INSTANCE_KEY, TARGET_INSTANCE_KEY );

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

	public static final Scope DEPENDENCY_TYPE = uniqueBy( DEPENDENCY_TYPE_KEY );
	public static final Scope DEPENDENCY_INSTANCE = uniqueBy( DEPENDENCY_INSTANCE_KEY );
	public static final Scope TARGET_INSTANCE = uniqueBy( TARGET_INSTANCE_KEY );
	public static final Scope DEPENDENCY = uniqueBy( TARGETED_DEPENDENCY_TYPE_KEY );

	public static Scope uniqueBy( DependencyProperty keyDeduction ) {
		return new DependencyPropertyScope( keyDeduction );
	}

	public static Repository asSnapshot( Repository src, Repository dest ) {
		return new SnapshotRepository( src, dest );
	}

	/**
	 * What is usually called a 'default'-{@link Scope} will ask the {@link Provider} passed each
	 * time the {@link Repository#serve(Dependency, InjectronInfo, Provider)}}-method is invoked.
	 * 
	 * The {@link Scope} is also used as {@link Repository} instance since both don#t have any
	 * state.
	 * 
	 * @see Scoped#INJECTION
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
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
		public <T> T serve( Dependency<? super T> dependency, InjectronInfo<T> info, Provider<T> provider ) {
			return provider.provide();
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
		public <T> T serve(Dependency<? super T> dependency, InjectronInfo<T> info, Provider<T> provider) {
			Repository repository = threadRepository.get();
			if ( repository == null ) {
				// since each thread is just accessing its own repo there cannot be a repo set for the running thread after we checked for null
				repository = repositoryScope.init();
				threadRepository.set( repository );
			}
			return repository.serve( dependency, info, provider );
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
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	private static final class SnapshotRepository
			implements Repository {

		private final Repository src;
		private final Repository dest;

		SnapshotRepository( Repository src, Repository dest ) {
			super();
			this.src = src;
			this.dest = dest;
		}

		@Override
		public <T> T serve(Dependency<? super T> dependency, InjectronInfo<T> info, Provider<T> provider) {
			return dest.serve( dependency, info, new SnapshotingProvider<>( dependency, info, provider, src ) );
		}

		private static final class SnapshotingProvider<T>
				implements Provider<T> {

			private final Dependency<? super T> dependency;
			private final InjectronInfo<T> info;
			private final Provider<T> supplier;
			private final Repository src;

			SnapshotingProvider(Dependency<? super T> dependency, InjectronInfo<T> info, Provider<T> supplier, Repository src) {
				super();
				this.dependency = dependency;
				this.info = info;
				this.supplier = supplier;
				this.src = src;
			}

			@Override
			public T provide() {
				return src.serve(dependency, info, supplier);
			}
		}

	}

	private static final class DependencyPropertyScope
			implements Scope {

		private final DependencyProperty property;

		DependencyPropertyScope( DependencyProperty property ) {
			super();
			this.property = property;
		}

		@Override
		public Repository init() {
			return new DependencyPropertyRepository( property );
		}

		@Override
		public String toString() {
			return "(per-" + property + ")";
		}

	}

	private static final class CombinedProperty
			implements DependencyProperty {

		private final DependencyProperty first;
		private final DependencyProperty second;

		CombinedProperty( DependencyProperty first, DependencyProperty second ) {
			super();
			this.first = first;
			this.second = second;
		}

		@Override
		public <T> String deriveFrom( Dependency<T> dependency ) {
			return first.deriveFrom( dependency ).concat( second.deriveFrom( dependency ) );
		}

	}

	private static final class TargetInstanceProperty
			implements DependencyProperty {

		TargetInstanceProperty() {
			// make visible
		}

		@Override
		public <T> String deriveFrom( Dependency<T> dependency ) {
			StringBuilder b = new StringBuilder();
			for ( int i = dependency.injectionDepth() - 1; i >= 0; i-- ) {
				b.append( dependency.target( i ) );
			}
			return b.toString();
		}

		@Override
		public String toString() {
			return "target-instance";
		}

	}

	private static final class DependencyTypeProperty
			implements DependencyProperty {

		DependencyTypeProperty() {
			// make visible
		}

		@Override
		public <T> String deriveFrom( Dependency<T> dependency ) {
			return dependency.type().toString();
		}

		@Override
		public String toString() {
			return "dependendy-type";
		}

	}

	private static final class DependencyInstanceProperty
			implements DependencyProperty {

		DependencyInstanceProperty() {
			// make visible
		}

		@Override
		public <T> String deriveFrom( Dependency<T> dependency ) {
			return dependency.instance.name.toString() + "@" + dependency.type().toString();
		}

		@Override
		public String toString() {
			return "dependendy-type";
		}
	}

	// e.g. get receiver class from dependency -to be reusable the provider could offer a identity --> a wrapper class would be needed anyway so maybe best is to have quite similar impl. all using a identity hash-map

	private static final class DependencyPropertyRepository
			implements Repository {

		private final Map<String, Object> instances = new HashMap<>();
		private final DependencyProperty property;

		DependencyPropertyRepository( DependencyProperty injectionKey ) {
			super();
			this.property = injectionKey;
		}

		@Override
		@SuppressWarnings ( "unchecked" )
		public <T> T serve(Dependency<? super T> dependency, InjectronInfo<T> info, Provider<T> provider) {
			final String key = property.deriveFrom( dependency );
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
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 * 
	 */
	private static final class ApplicationScope
			implements Scope {

		ApplicationScope() {
			//make visible
		}

		@Override
		public Repository init() {
			return new LazyInjectronRepository();
		}

		@Override
		public String toString() {
			return "(per-app)";
		}
	}

	/**
	 * Contains once instance per {@link Injectron}. Instances are never
	 * updated. This can be used to create a thread, request or application
	 * {@link Scope}.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	private static final class LazyInjectronRepository
			implements Repository {

		private Object[] instances;

		LazyInjectronRepository() {
			super();
		}

		@Override
		@SuppressWarnings ( "unchecked" )
		public <T> T serve(Dependency<? super T> dependency, InjectronInfo<T> info, Provider<T> provider) {
			if ( instances == null ) {
				instances = new Object[info.count];
			}
			final int serialID = info.serialID;
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
