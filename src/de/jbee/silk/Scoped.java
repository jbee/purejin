package de.jbee.silk;

public class Scoped {

	/**
	 * Asks the {@link ResourceResolver} once per injection.
	 */
	public static final Scope DEFAULT = new DefaultScope();
	/**
	 * Asks the {@link ResourceResolver} once per binding. Thereby instances become singletons local
	 * to the application.
	 */
	public static final Scope APPLICATION = new ApplicationScope();
	/**
	 * Asks the {@link ResourceResolver} once per thread per binding which is understand commonly as
	 * a usual 'per-thread' singleton.
	 */
	public static final Scope THREAD = new ThreadScope( new ThreadLocal<Repository>(), APPLICATION );

	static Repository makeConsistent( Repository work, Repository bound ) {
		return new ConsistentRepository( work, bound );
	}

	static final class DefaultScope
			implements Scope, Repository {

		@Override
		public Repository init( int cardinality ) {
			return this;
		}

		@Override
		public <T> T yield( Dependency<T> dependency, ResourceResolver<T> resolver ) {
			return resolver.resolve( dependency );
		}

	}

	static final class ThreadScope
			implements Scope, Repository {

		private final ThreadLocal<Repository> threadRepository;
		private final Scope repositoryScope;

		ThreadScope( ThreadLocal<Repository> threadRepository, Scope repositoryScope ) {
			super();
			this.threadRepository = threadRepository;
			this.repositoryScope = repositoryScope;
		}

		@Override
		public <T> T yield( Dependency<T> dependency, ResourceResolver<T> resolver ) {
			Repository repository = threadRepository.get();
			if ( repository == null ) {
				// since each thread is just accessing its own repo there cannot be a repo set for the running thread after we checked for null
				repository = repositoryScope.init( dependency.resourceCardinality() );
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
	 * The 'work'-{@link Repository} will be asked first given the {@linkplain Repository}
	 * originally bound as {@link Provider}. When the instance isn't found in the work repository
	 * the originally bound will be asked. It could be found there or will be provided by the
	 * original provider given. Both repositories will remember the resolved instance whereby the
	 * repository considered as the work-repository will deliver a consistent image of the world as
	 * long as it exists.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	static final class ConsistentRepository
			implements Repository {

		private final Repository work;
		private final Repository bound;

		ConsistentRepository( Repository work, Repository bound ) {
			super();
			this.work = work;
			this.bound = bound;
		}

		@Override
		public <T> T yield( Dependency<T> dependency, ResourceResolver<T> resolver ) {
			return work.yield( dependency, new ConsistentResourceResolver<T>( resolver, bound ) );
		}

		private static final class ConsistentResourceResolver<T>
				implements ResourceResolver<T> {

			private final ResourceResolver<T> resolver;
			private final Repository bound;

			ConsistentResourceResolver( ResourceResolver<T> resolver, Repository bound ) {
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
		public <T> T yield( Dependency<T> dependency, ResourceResolver<T> resolver ) {
			// e.g. get receiver class from dependency -to be reusable the provider could offer a identity --> a wrapper class would be needed anyway so maybe best is to have quite similar impl. all using a identity hash-map
			// TODO Auto-generated method stub
			return null;
		}

	}

	static final class ApplicationScope
			implements Scope {

		@Override
		public Repository init( int cardinality ) {
			return new PerBindingRepository( new Object[cardinality] );
		}

	}

	static final class PerBindingRepository
			implements Repository {

		private final Object[] instances;

		PerBindingRepository( Object[] instances ) {
			super();
			this.instances = instances;
		}

		@Override
		public <T> T yield( Dependency<T> dependency, ResourceResolver<T> resolver ) {
			@SuppressWarnings ( "unchecked" )
			T res = (T) instances[dependency.resourceNr()];
			if ( res != null ) {
				return res;
			}
			res = resolver.resolve( dependency );
			instances[dependency.resourceNr()] = res;
			return res;
		}
	}

}
