/*
 *  Copyright (c) 2012-2017, Jan Bernitt 
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
public final class BoundParameter<T> implements Parameter<T> {

	public enum ParameterType { INSTANCE, CONSTANT, EXTERNAL }
	
	private static final BoundParameter<?>[] NO_PARAMS = new BoundParameter<?>[0];

	public static <T> BoundParameter<T> bind( Parameter<T> parameter ) {
		if ( parameter instanceof BoundParameter<?> ) {
			return (BoundParameter<T>) parameter;
		}
		if ( parameter instanceof Instance<?> ) {
			Instance<T> i = (Instance<T>) parameter;
			return new BoundParameter<>(ParameterType.INSTANCE, i.type(), i, null, Supply.instance( i ) );
		}
		if ( parameter instanceof Type<?> ) {
			Instance<T> i = anyOf( (Type<T>) parameter );
			return new BoundParameter<>(ParameterType.INSTANCE, i.type(), i, null, Supply.instance( i ) );
		}
		if ( parameter instanceof Dependency<?> ) {
			final Dependency<T> d = (Dependency<T>) parameter;
			return new BoundParameter<>(ParameterType.EXTERNAL, d.type(), d.instance, null, Supply.dependency( d ) );
		}
		throw new IllegalArgumentException( "Unknown parameter type:" + parameter );
	}

	public static <T> Parameter<T> constant( Class<T> type, T constant ) {
		return constant( raw( type ), constant );
	}

	public static <T> Parameter<T> constant( Type<T> type, T constant ) {
		return new BoundParameter<>(ParameterType.CONSTANT, type, Instance.defaultInstanceOf(type), constant, Supply.constant( constant ) );
	}

	public static <T> Parameter<T> supplier( Type<T> type, Supplier<? extends T> supplier ) {
		return new BoundParameter<>(ParameterType.EXTERNAL, type, Instance.defaultInstanceOf(type), null, supplier );
	}

	public static <S, T extends S> Parameter<S> asType( Class<S> supertype, Parameter<T> parameter ) {
		return asType( raw( supertype ), parameter );
	}

	public static <S, T extends S> Parameter<S> asType( Type<S> supertype, Parameter<T> parameter ) {
		return bind( parameter ).typed(supertype);
	}

	@SafeVarargs
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
					found = parameter.type().isAssignableTo( types[i] );
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
	
	public final ParameterType type;
	public final Type<T> asType;
	public final Instance<? extends T> instance;
	public final T value;
	public final Supplier<? extends T> supplier;

	public BoundParameter(ParameterType type, Type<T> asType, Instance<? extends T> instance, T value, Supplier<? extends T> supplier) {
		super();
		this.type = asType.rawType == Injector.class ? ParameterType.EXTERNAL : type;
		this.asType = asType;
		this.instance = instance;
		this.value = value;
		this.supplier = supplier;
	}

	@Override
	public Type<T> type() {
		return asType;
	}
	
	public BoundParameter<T> external() {
		return new BoundParameter<>(ParameterType.EXTERNAL, asType, instance, value, supplier);
	}

	/**
	 * @param type
	 *            The new type of this {@link BoundParameter}
	 * @throws ClassCastException
	 *             In case the given type is incompatible with the previous one.
	 */
	@SuppressWarnings ( "unchecked" )
	@Override
	public <E> BoundParameter<E> typed( Type<E> type ) throws ClassCastException {
		asType.toSupertype(type);
		return new BoundParameter<>(this.type, type, (Instance<E>)instance, (E)value, (Supplier<? extends E>) supplier );
	}

	@Override
	public String toString() {
		return Supply.describe( asType, supplier );
	}
	
}
