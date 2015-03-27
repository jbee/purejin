/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.raw;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Parameter;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;

/**
 * A {@link BoundParameter} is a {@link Supplier} for parameters of
 * {@link Constructor} or {@link Method} invocations.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T>
 *            The {@link Type} of the parameter
 */
public final class BoundParameter<T> implements Parameter<T>, Supplier<T> {

	private static final BoundParameter<?>[] NO_PARAMS = new BoundParameter<?>[0];

	public static <T> BoundParameter<T> bind( Parameter<T> parameter ) {
		if ( parameter instanceof BoundParameter<?> ) {
			return (BoundParameter<T>) parameter;
		}
		if ( parameter instanceof Instance<?> ) {
			Instance<T> i = (Instance<T>) parameter;
			return new BoundParameter<T>( i.getType(), Supply.instance( i ) );
		}
		if ( parameter instanceof Type<?> ) {
			Instance<T> i = anyOf( (Type<T>) parameter );
			return new BoundParameter<T>( i.getType(), Supply.instance( i ) );
		}
		if ( parameter instanceof Dependency<?> ) {
			final Dependency<T> d = (Dependency<T>) parameter;
			return new BoundParameter<T>( d.getType(), Supply.dependency( d ) );
		}
		throw new IllegalArgumentException( "Unknown parameter type:" + parameter );
	}

	public static <T> Parameter<T> constant( Class<T> type, T constant ) {
		return constant( raw( type ), constant );
	}

	public static <T> Parameter<T> constant( Type<T> type, T constant ) {
		return new BoundParameter<T>( type, Supply.constant( constant ) );
	}

	public static <T> Parameter<T> supplier( Type<T> type, Supplier<? extends T> supplier ) {
		return new BoundParameter<T>( type, supplier );
	}

	public static <S, T extends S> Parameter<S> asType( Class<S> supertype, Parameter<T> parameter ) {
		return asType( raw( supertype ), parameter );
	}

	public static <S, T extends S> Parameter<S> asType( Type<S> supertype, Parameter<T> parameter ) {
		return new BoundParameter<S>( supertype, bind( parameter ) );
	}

	public static <E> BoundParameter<? extends E>[] bind(Parameter<? extends E>... parameters ) {
		@SuppressWarnings ( "unchecked" )
		BoundParameter<? extends E>[] params = new BoundParameter[parameters.length];
		for ( int i = 0; i < parameters.length; i++ ) {
			params[i] = bind( parameters[i] );
		}
		return params;
	}

	public static BoundParameter<?>[] bind( Type<?>[] types, Parameter<?>... parameters ) {
		if ( types.length == 0 ) {
			return NO_PARAMS;
		}
		BoundParameter<?>[] params = new BoundParameter<?>[types.length];
		for ( Parameter<?> parameter : parameters ) {
			int i = 0;
			boolean found = false;
			while ( i < types.length && !found ) {
				if ( params[i] == null ) {
					found = parameter.isAssignableTo( types[i] );
					if ( found ) {
						params[i] = bind( parameter );
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
				params[i] = bind( types[i] );
			}
		}
		return params;
	}	
	
	
	//------------------------------------------------------
	
	private final Type<T> supplied;
	private final Supplier<? extends T> supplier;

	BoundParameter( Type<T> type, Supplier<? extends T> supplier ) {
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

	/**
	 * @param type
	 *            The new type of this {@link BoundParameter}
	 * @throws UnsupportedOperationException
	 *             In case the given type is incompatible with the previous one.
	 */
	@SuppressWarnings ( "unchecked" )
	@Override
	public <E> BoundParameter<E> typed( Type<E> type )
			throws UnsupportedOperationException {
		if ( supplied.isAssignableTo( type ) ) {
			return new BoundParameter<E>( type, (Supplier<? extends E>) supplier );
		}
		throw new UnsupportedOperationException( "Only supertypes of " + supplied
				+ " can be supplied as same paramter - but was: " + type );
	}

	@Override
	public String toString() {
		return Supply.describe( supplied, supplier );
	}
	
}
