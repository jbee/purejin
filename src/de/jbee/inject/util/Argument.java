package de.jbee.inject.util;

import de.jbee.inject.Dependency;
import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Instance;

public abstract class Argument<T> {

	public abstract T resolve( Dependency<?> parent, DependencyResolver context );

	public static <T> Argument<T> arg( Instance<T> instance ) {
		return new InstanceArgument<T>( instance );
	}

	public static <T> Argument<T> arg( T constant ) {
		return new ConstantArgument<T>( constant );
	}

	public static <T> Argument<T> arg( Dependency<? extends T> dependency ) {
		return new DependencyArgument<T>( dependency );
	}

	private Argument() {
		// not allow further extending outside of this class
	}

	private static final class InstanceArgument<T>
			extends Argument<T> {

		private final Instance<T> instance;

		@SuppressWarnings ( "synthetic-access" )
		InstanceArgument( Instance<T> instance ) {
			super();
			this.instance = instance;
		}

		@Override
		public T resolve( Dependency<?> parent, DependencyResolver context ) {
			return context.resolve( parent.instanced( instance ) );
		}

	}

	private static final class ConstantArgument<T>
			extends Argument<T> {

		private final T constant;

		ConstantArgument( T constant ) {
			super();
			this.constant = constant;
		}

		@Override
		public T resolve( Dependency<?> parent, DependencyResolver context ) {
			return constant;
		}

	}

	private static final class DependencyArgument<T>
			extends Argument<T> {

		private final Dependency<? extends T> dependency;

		DependencyArgument( Dependency<? extends T> dependency ) {
			super();
			this.dependency = dependency;
		}

		@Override
		public T resolve( Dependency<?> parent, DependencyResolver context ) {
			return context.resolve( dependency );
		}

	}
}
