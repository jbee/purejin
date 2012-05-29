package de.jbee.inject;

public class Scoped {

	/**
	 * Asks the {@link Injectable} once per injection.
	 */
	public static final Scope DEFAULT = new DefaultScope();
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

	public static Repository asSnapshot( Repository src, Repository dest ) {
		return new SnapshotRepository( src, dest );
	}

	private static final class SnapshotScope
			implements Scope {

		private final Scope dest;
		private final Scope src;

		SnapshotScope( Scope synchronous, Scope asynchronous ) {
			super();
			this.dest = synchronous;
			this.src = asynchronous;
		}

		@Override
		public Repository init( int cardinality ) {
			return new SnapshotRepository( src.init( cardinality ), dest.init( cardinality ) );
		}

		@Override
		public String toString() {
			return src + "->" + dest;
		}
	}

	/**
	 * What is usually called a 'default'-{@link Scope} will ask the {@link Injectable} passed each
	 * time the {@link Repository#yield(Dependency, Injectable)}-method is invoked.
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
		public <T> T yield( Injection<T> injection, Injectable<T> injectable ) {
			return injectable.instanceFor( injection );
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
		public <T> T yield( Injection<T> injection, Injectable<T> injectable ) {
			Repository repository = threadRepository.get();
			if ( repository == null ) {
				// since each thread is just accessing its own repo there cannot be a repo set for the running thread after we checked for null
				repository = repositoryScope.init( injection.injectronCardinality() );
				threadRepository.set( repository );
			}
			return repository.yield( injection, injectable );
		}

		@Override
		public Repository init( int cardinality ) {
			return this;
		}

		@Override
		public String toString() {
			return "per thread";
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
		public <T> T yield( Injection<T> injection, Injectable<T> injectable ) {
			return dest.yield( injection, new SnapshotingSupplier<T>( injectable, src ) );
		}

		private static final class SnapshotingSupplier<T>
				implements Injectable<T> {

			private final Injectable<T> supplier;
			private final Repository src;

			SnapshotingSupplier( Injectable<T> supplier, Repository src ) {
				super();
				this.supplier = supplier;
				this.src = src;
			}

			@Override
			public T instanceFor( Injection<T> injection ) {
				return src.yield( injection, supplier );
			}

		}

	}

	static final class PerIdentityRepository
			implements Repository {

		@Override
		public <T> T yield( Injection<T> injection, Injectable<T> injectable ) {
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

		@Override
		public String toString() {
			return "#";
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

		private final Object[] instances;

		ResourceRepository( Object[] instances ) {
			super();
			this.instances = instances;
		}

		@Override
		@SuppressWarnings ( "unchecked" )
		public <T> T yield( Injection<T> injection, Injectable<T> injectable ) {
			T res = (T) instances[injection.injectronSerialNumber()];
			if ( res != null ) {
				return res;
			}
			// just sync the (later) unexpected path that is executed once
			synchronized ( instances ) {
				res = (T) instances[injection.injectronSerialNumber()];
				if ( res != null ) { // we need to ask again since the instance could have been initialized before we got entrance to the sync block
					res = injectable.instanceFor( injection );
					instances[injection.injectronSerialNumber()] = res;
				}
			}
			return res;
		}

	}

}
