/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.raw;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Parameter;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.util.Parameterization;

/**
 * A utility to supply {@link Parameter}s during the binding.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Parameterize {

	@SuppressWarnings ( "unchecked" )
	public static <T> Parameterization<T> parameterization( Parameter<T> parameter ) {
		if ( parameter instanceof Parameterization<?> ) {
			return (Parameterization<T>) parameter;
		}
		if ( parameter instanceof Instance<?> ) {
			Instance<T> i = (Instance<T>) parameter;
			return new SuppliedParameter<T>( i.getType(), SuppliedBy.instance( i ) );
		}
		if ( parameter instanceof Type<?> ) {
			Instance<T> i = anyOf( (Type<T>) parameter );
			return new SuppliedParameter<T>( i.getType(), SuppliedBy.instance( i ) );
		}
		if ( parameter instanceof Dependency<?> ) {
			final Dependency<T> d = (Dependency<T>) parameter;
			return new SuppliedParameter<T>( d.getType(), SuppliedBy.dependency( d ) );
		}
		throw new IllegalArgumentException( "Unknown parameter type:" + parameter );
	}

	public static <T> Parameter<T> constant( Class<T> type, T constant ) {
		return constant( raw( type ), constant );
	}

	public static <T> Parameter<T> constant( Type<T> type, T constant ) {
		return new SuppliedParameter<T>( type, SuppliedBy.constant( constant ) );
	}

	public static <T> Parameter<T> supplier( Type<T> type, Supplier<? extends T> supplier ) {
		return new SuppliedParameter<T>( type, supplier );
	}

	public static <S, T extends S> Parameter<S> asType( Class<S> supertype, Parameter<T> parameter ) {
		return asType( raw( supertype ), parameter );
	}

	public static <S, T extends S> Parameter<S> asType( Type<S> supertype, Parameter<T> parameter ) {
		return new SuppliedParameter<S>( supertype, parameterization( parameter ) );
	}

	public static Parameterization<?>[] parameterizations( Type<?>[] types,
			Parameter<?>... parameters ) {
		Parameterization<?>[] params = new Parameterization<?>[types.length];
		for ( Parameter<?> parameter : parameters ) {
			int i = 0;
			boolean found = false;
			while ( i < types.length && !found ) {
				if ( params[i] == null ) {
					found = parameter.isAssignableTo( types[i] );
					if ( found ) {
						params[i] = parameterization( parameter );
					}
				}
				i++;
			}
			if ( !found ) {
				throw new IllegalArgumentException( "Couldn't arrange parameter: " + parameter );
			}
		}
		for ( int i = 0; i < params.length; i++ ) {
			if ( params[i] == null ) {
				params[i] = parameterization( types[i] );
			}
		}
		return params;
	}

	private static final class SuppliedParameter<T>
			implements Parameter<T>, Parameterization<T> {

		private final Type<T> supplied;
		private final Supplier<? extends T> supplier;

		SuppliedParameter( Type<T> type, Supplier<? extends T> supplier ) {
			super();
			this.supplied = type;
			this.supplier = supplier;
		}

		@Override
		public boolean isAssignableTo( Type<?> type ) {
			return supplied.isAssignableTo( type );
		}

		@Override
		public T supply( Dependency<? super T> dependency, Injector injector ) {
			return supplier.supply( dependency, injector );
		}

		@Override
		public Type<T> getType() {
			return supplied;
		}

		@SuppressWarnings ( "unchecked" )
		@Override
		public <E> Parameterization<E> typed( Type<E> type )
				throws UnsupportedOperationException {
			if ( supplied.isAssignableTo( type ) ) {
				return new SuppliedParameter<E>( type, (Supplier<? extends E>) supplier );
			}
			throw new UnsupportedOperationException( "Only supertypes of " + supplied
					+ " can be supplied as same paramter - but was: " + type );
		}

		@Override
		public String toString() {
			return "<" + supplied + ":" + supplier + ">";
		}
	}
}
