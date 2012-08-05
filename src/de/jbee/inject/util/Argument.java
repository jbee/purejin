package de.jbee.inject.util;

import de.jbee.inject.Dependency;
import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Instance;
import de.jbee.inject.Parameter;
import de.jbee.inject.Type;

public abstract class Argument<T> {

	public abstract T resolve( Dependency<?> parent, DependencyResolver context );

	public static <T> Argument<T> argumentFor( Parameter<T> parameter ) {
		if ( parameter instanceof Instance<?> ) {
			return new InstanceArgument<T>( (Instance<T>) parameter );
		} else if ( parameter instanceof Type<?> ) {
			return new InstanceArgument<T>( Instance.anyOf( (Type<T>) parameter ) );
		} else if ( parameter instanceof Dependency<?> ) {
			return new DependencyArgument<T>( (Dependency<T>) parameter );
		} else {
			//TODO add asType and constant parameters
			throw new IllegalArgumentException( "Unknown parameter type:" + parameter );
		}
	}

	public static <T> Parameter<T> parameter( T constant, Type<T> type ) {

		return null;
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

		@SuppressWarnings ( "synthetic-access" )
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

		@SuppressWarnings ( "synthetic-access" )
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
