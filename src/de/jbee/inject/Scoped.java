package de.jbee.inject;

public class Scoped {

	/**
	 * Asks the {@link DependencyResolver} once per injection.
	 */
	public static final Scope DEFAULT = new DefaultScope();
	/**
	 * Asks the {@link DependencyResolver} once per binding. Thereby instances become singletons
	 * local to the application.
	 */
	public static final Scope APPLICATION = new ApplicationScope();
	/**
	 * Asks the {@link DependencyResolver} once per thread per binding which is understand commonly
	 * as a usual 'per-thread' singleton.
	 */
	public static final Scope THREAD = new ThreadScope( new ThreadLocal<Repository>(), APPLICATION );

	public static Repository asSnapshot( Repository synchronous, Repository asynchronous ) {
		return new SnapshotRepository( synchronous, asynchronous );
	}

	static final class SnapshotScope
			implements Scope {

		private final Scope synchronous;
		private final Scope asynchronous;

		SnapshotScope( Scope synchronous, Scope asynchronous ) {
			super();
			this.synchronous = synchronous;
			this.asynchronous = asynchronous;
		}

		@Override
		public Repository init( int cardinality ) {
			return new SnapshotRepository( synchronous.init( cardinality ),
					asynchronous.init( cardinality ) );
		}
	}

	/**
	 * What is usually called a 'default'-{@link Scope} will ask the {@link DependencyResolver}
	 * passed each time the {@link Repository#yield(Dependency, DependencyResolver)}-method is
	 * invoked.
	 * 
	 * The {@link Scope} is also used as {@link Repository} instance since both don#t have any
	 * state.
	 * 
	 * @see Scoped#DEFAULT
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	private static final class DefaultScope
			implements Scope, Repository {

		DefaultScope() {
			// make visible
		}

		@Override
		public Repository init( int cardinality ) {
			return this;
		}

		@Override
		public <T> T yield( Dependency<T> dependency, DependencyResolver<T> resolver ) {
			return resolver.resolve( dependency );
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
		public <T> T yield( Dependency<T> dependency, DependencyResolver<T> resolver ) {
			Repository repository = threadRepository.get();
			if ( repository == null ) {
				// since each thread is just accessing its own repo there cannot be a repo set for the running thread after we checked for null
				repository = repositoryScope.init( dependency.injectronCardinality() );
				threadRepository.set( repository );
			}
			return repository.yield( dependency, resolver );
		}

		@Override
		public Repository init( int cardinality ) {
			return this;
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

		private final Repository synchronous;
		private final Repository asynchronous;

		SnapshotRepository( Repository synchronous, Repository asynchronous ) {
			super();
			this.synchronous = synchronous;
			this.asynchronous = asynchronous;
		}

		@Override
		public <T> T yield( Dependency<T> dependency, DependencyResolver<T> resolver ) {
			return synchronous.yield( dependency, new SnapshottedResourceResolver<T>( resolver,
					asynchronous ) );
		}

		private static final class SnapshottedResourceResolver<T>
				implements DependencyResolver<T> {

			private final DependencyResolver<T> resolver;
			private final Repository bound;

			SnapshottedResourceResolver( DependencyResolver<T> resolver, Repository bound ) {
				super();
				this.resolver = resolver;
				this.bound = bound;
			}

			@Override
			public T resolve( Dependency<T> dependency ) {
				return bound.yield( dependency, resolver );
			}

		}
	}

	static final class PerIdentityRepository
			implements Repository {

		@Override
		public <T> T yield( Dependency<T> dependency, DependencyResolver<T> resolver ) {
			// e.g. get receiver class from dependency -to be reusable the provider could offer a identity --> a wrapper class would be needed anyway so maybe best is to have quite similar impl. all using a identity hash-map
			// TODO Auto-generated method stub
			return null;
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
		public Repository init( int cardinality ) {
			return new ResourceRepository( new Object[cardinality] );
		}

	}

	/**
	 * Contains once instance per resource. Resources are never updates. This can for example be
	 * used to create a thread or request {@link Scope}.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	private static final class ResourceRepository
			implements Repository {

		private final Object[] instances;

		ResourceRepository( Object[] instances ) {
			super();
			this.instances = instances;
		}

		@Override
		@SuppressWarnings ( "unchecked" )
		public <T> T yield( Dependency<T> dependency, DependencyResolver<T> resolver ) {
			T res = (T) instances[dependency.injectronSerialNumber()];
			if ( res != null ) {
				return res;
			}
			// just sync the (later) unexpected path that is executed once
			synchronized ( instances ) {
				res = (T) instances[dependency.injectronSerialNumber()];
				if ( res != null ) { // we need to ask again since the instance could have been initialized before we got entrance to the sync block
					res = resolver.resolve( dependency );
					instances[dependency.injectronSerialNumber()] = res;
				}
			}
			return res;
		}
	}

}
