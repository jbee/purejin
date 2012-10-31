/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.util;

import static se.jbee.inject.Type.raw;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Parameter;
import se.jbee.inject.Type;

public final class Argument<T>
		implements Parameter<T> {

	public static <T> Argument<T> argumentFor( Parameter<T> parameter ) {
		if ( parameter instanceof Argument<?> ) {
			return (Argument<T>) parameter;
		}
		if ( parameter instanceof Instance<?> ) {
			return new Argument<T>( (Instance<T>) parameter );
		}
		if ( parameter instanceof Type<?> ) {
			return new Argument<T>( Instance.anyOf( (Type<T>) parameter ) );
		}
		if ( parameter instanceof Dependency<?> ) {
			return new Argument<T>( (Dependency<T>) parameter );
		}
		throw new IllegalArgumentException( "Unknown parameter type:" + parameter );
	}

	public static <T> Parameter<T> constant( Class<T> type, T constant ) {
		return constant( raw( type ), constant );
	}

	public static <T> Parameter<T> constant( Type<T> type, T constant ) {
		return new Argument<T>( constant, type, null, null );
	}

	public static <S, T extends S> Parameter<S> asType( Class<S> supertype, Parameter<T> parameter ) {
		return asType( raw( supertype ), parameter );
	}

	public static <S, T extends S> Parameter<S> asType( Type<S> supertype, Parameter<T> parameter ) {
		Argument<T> arg = argumentFor( parameter );
		return new Argument<S>( arg.constant, supertype, arg.instance, arg.dependency );
	}

	public static boolean allConstants( Argument<?>[] args ) {
		for ( Argument<?> arg : args ) {
			if ( !arg.isConstant() ) {
				return false;
			}
		}
		return true;
	}

	public static Object[] constantsFrom( Argument<?>[] args ) {
		Object[] consts = new Object[args.length];
		for ( int i = 0; i < args.length; i++ ) {
			consts[i] = args[i].constant;
		}
		return consts;
	}

	public static <T> Object[] resolve( Dependency<? super T> dependency, Injector context,
			Argument<?>[] arguments ) {
		Object[] args = new Object[arguments.length];
		for ( int i = 0; i < arguments.length; i++ ) {
			args[i] = arguments[i].resolve( dependency, context );
		}
		return args;
	}

	private final T constant;
	private final Type<? super T> asType;
	private final Instance<? extends T> instance;
	private final Dependency<? extends T> dependency;

	private Argument( Instance<T> instance ) {
		this( null, instance.getType(), instance, null );
	}

	private Argument( Dependency<T> dependency ) {
		this( null, dependency.getType(), null, dependency );
	}

	private Argument( T constant, Type<? super T> asType, Instance<? extends T> instance,
			Dependency<? extends T> dependency ) {
		super();
		this.constant = constant;
		this.asType = asType;
		this.instance = instance;
		this.dependency = dependency;
	}

	public T resolve( Dependency<?> constructed, Injector context ) {
		if ( constant != null ) {
			return constant;
		}
		if ( dependency != null ) {
			return context.resolve( dependency );
		}
		return context.resolve( constructed.instanced( instance ) );
	}

	public boolean isConstant() {
		return constant != null;
	}

	@Override
	public boolean isAssignableTo( Type<?> type ) {
		return asType.isAssignableTo( type );
	}

	@Override
	public String toString() {
		String as = " as " + asType.toString();
		if ( isConstant() ) {
			return constant.toString() + as;
		}
		if ( dependency != null ) {
			return dependency.toString() + as;
		}
		return instance.toString() + as;
	}

	public static Argument<?>[] arguments( Type<?>[] types, Parameter<?>... parameters ) {
		Argument<?>[] arguments = new Argument<?>[types.length];
		for ( Parameter<?> parameter : parameters ) {
			int i = 0;
			boolean found = false;
			while ( i < types.length && !found ) {
				if ( arguments[i] == null ) {
					found = parameter.isAssignableTo( types[i] );
					if ( found ) {
						arguments[i] = argumentFor( parameter );
					}
				}
				i++;
			}
			if ( !found ) {
				throw new IllegalArgumentException( "Couldn't understand parameter: " + parameter );
			}
		}
		for ( int i = 0; i < arguments.length; i++ ) {
			if ( arguments[i] == null ) {
				arguments[i] = argumentFor( types[i] );
			}
		}
		return arguments;
	}

}
