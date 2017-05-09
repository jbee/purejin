/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Type.raw;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

import se.jbee.inject.Array;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Packages;
import se.jbee.inject.Parameter;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency.NoMethodForDependency;
import se.jbee.inject.container.Inject;

/**
 * The basic {@link Inspector} implementations. It allows to chose {@link Constructor}s and
 * {@link Method}s based on the {@link Annotation}s present, the {@link Type}s a method returns or
 * the {@link Packages} they are defined in.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class Inspect
		implements Inspector {

	/**
	 * By default just the constructor is inspected. The {@link Inspector} will pick the
	 * {@link #defaultConstructor(Class)}.
	 */
	public static final Inspect DEFAULT = all().constructors();

	private static final Parameter<?>[] NO_PARAMETERS = new Parameter<?>[0];
	private static final Method[] NO_METHODS = new Method[0];

	/**
	 * @return a {@link Inspector} that will result in all methods and a constructor for all given
	 *         implementation classes. The result can be further restricted using any combination of
	 *         the instance methods of {@link Inject}.
	 */
	public static Inspect all() {
		return new Inspect( false, true, true, Packages.ALL, Type.OBJECT, null, null );
	}

	/**
	 * @return same as {@link #all()} but result is already restricted to just static methods.
	 */
	public static Inspect allStatic() {
		return new Inspect( true, true, false, Packages.ALL, Type.OBJECT, null, null );
	}

	public static Inspect methodsReturn( Type<?> returnType ) {
		return all().methods().returnTypeAssignableTo( returnType );
	}

	private final boolean statics;
	private final boolean methods;
	private final boolean constructors;
	private final Packages packages;
	private final Type<?> assignable;
	private final Class<? extends Annotation> accessible;
	private final Class<? extends Annotation> namedby;

	private Inspect( boolean statics, boolean methods, boolean constructors, Packages packages,
			Type<?> assignable, Class<? extends Annotation> accessible,
			Class<? extends Annotation> namedBy ) {
		super();
		this.methods = methods;
		this.constructors = constructors;
		this.statics = statics;
		this.accessible = accessible;
		this.packages = packages;
		this.namedby = namedBy;
		this.assignable = assignable;
	}

	@Override
	public Parameter<?>[] parametersFor( AccessibleObject obj ) {
		if ( namedby == null ) {
			return NO_PARAMETERS;
		}
		if ( obj instanceof Method ) {
			Method method = (Method) obj;
			return parametersFor( Type.parameterTypes( method ), method.getParameterAnnotations() );
		}
		if ( obj instanceof Constructor<?> ) {
			Constructor<?> constructor = (Constructor<?>) obj;
			return parametersFor( Type.parameterTypes( constructor ),
					constructor.getParameterAnnotations() );
		}
		return NO_PARAMETERS;
	}

	private Parameter<?>[] parametersFor( Type<?>[] types, Annotation[][] annotations ) {
		List<Parameter<?>> res = new ArrayList<>();
		for ( int i = 0; i < annotations.length; i++ ) {
			Name name = Name.namedBy( namedby, annotations[i] );
			if ( name != Name.DEFAULT ) {
				res.add( Instance.instance( name, types[i] ) );
			}
		}
		return Array.of(res, Parameter.class);
	}

	@Override
	public Name nameFor( AccessibleObject obj ) {
		return Name.namedBy( namedby, obj );
	}

	@SuppressWarnings ( "unchecked" )
	@Override
	public <T> Constructor<T> constructorFor( Class<T> type ) {
		if ( constructors && packages.contains( Type.raw( type ) )
				&& Type.raw( type ).isAssignableTo( assignable ) ) {
			if ( accessible != null ) {
				for ( Constructor<?> c : type.getDeclaredConstructors() ) {
					if ( c.isAnnotationPresent( accessible ) ) {
						return (Constructor<T>) c;
					}
				}
			}
			try {
				return Inspect.defaultConstructor( type );
			} catch ( RuntimeException e ) {
				return null;
			}
		}
		return null;
	}

	@Override
	public <T> Method[] methodsIn( Class<T> implementor ) {
		if ( !methods ) {
			return NO_METHODS;
		}
		List<Method> res = new ArrayList<>();
		for ( Method m : implementor.getDeclaredMethods() ) {
			if ( matches( m ) ) {
				res.add( m );
			}
		}
		return Array.of( res, Method.class );
	}

	/**
	 * @return a {@link Inspector} restricted to inspect just methods (no constructors).
	 */
	public Inspect methods() {
		return new Inspect( statics, true, false, packages, assignable, accessible, namedby );
	}

	/**
	 * @return a {@link Inspector} restricted to inspect just constructors (no methods).
	 */
	public Inspect constructors() {
		return new Inspect( statics, false, true, packages, assignable, accessible, namedby );
	}

	/**
	 * @param annotation
	 *            An annotation available at runtime
	 * @return a {@link Inspector} restricted to inspect just constructors and/or methods where the
	 *         given annotation is present.
	 */
	public Inspect annotatedWith( Class<? extends Annotation> annotation ) {
		return new Inspect( statics, methods, constructors, packages, assignable, annotation,
				namedby );
	}

	/**
	 * @param annotation
	 *            An annotation available at runtime having at least one property of type
	 *            {@link String}.
	 * @return a {@link Inspector} that tries to extract an instance name from the given annotation.
	 */
	public Inspect namedBy( Class<? extends Annotation> annotation ) {
		return new Inspect( statics, methods, constructors, packages, assignable, accessible,
				annotation );
	}

	/**
	 * @param packages
	 *            The set of {@link Packages} the return type of a method or the class constructed
	 *            by a constructor should be contained in.
	 * @return a {@link Inspector} restricted to inspect just methods having a return type or a
	 *         constructor of a type that is a member of the set given.
	 */
	public Inspect returnTypeIn( Packages packages ) {
		return new Inspect( statics, methods, constructors, packages, assignable, accessible,
				namedby );
	}

	/**
	 * @param supertype
	 *            any {@link Type}
	 * @return a {@link Inspector} restricted to inspect just methods or constructors that return a
	 *         {@link Type} that is assignable to the given super-type.
	 */
	public Inspect returnTypeAssignableTo( Type<?> supertype ) {
		return new Inspect( statics, methods, constructors, packages, supertype, accessible,
				namedby );
	}

	/**
	 * Returns the constructor usually should be used.
	 * 
	 * @param declaringClass
	 *            constructed type
	 * @return The constructor with the most parameters.
	 * @throws NoMethodForDependency
	 *             in case the type is not constructible (has no constructors at all)
	 */
	public static <T> Constructor<T> defaultConstructor( Class<T> declaringClass ) throws NoMethodForDependency {
		Constructor<?>[] constructors = declaringClass.getDeclaredConstructors();
		if ( constructors.length == 0 ) {
			throw new NoMethodForDependency( raw(declaringClass) );
		}
		Constructor<?> mostArgConstructor = constructors[0];
		for ( int i = 0; i < constructors.length; i++ ) {
			if ( constructors[i].getParameterTypes().length > mostArgConstructor.getParameterTypes().length ) {
				mostArgConstructor = constructors[i];
			}
		}
		@SuppressWarnings ( "unchecked" )
		Constructor<T> c = (Constructor<T>) mostArgConstructor;
		return c;
	}

	public static <T> Constructor<T> noArgsConstructor( Class<T> declaringClass ) {
		if ( declaringClass.isInterface() ) {
			throw new NoMethodForDependency( raw(declaringClass) );
		}
		Constructor<T> c;
		try {
			c = declaringClass.getDeclaredConstructor();
		} catch ( Exception e ) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException)e;
			}
			throw new RuntimeException( e );
		}
		return c;
	}

	private boolean matches( Method m ) {
		if ( m.getGenericReturnType() instanceof TypeVariable<?> ) {
			return false;
		}
		Type<?> returnType = Type.returnType( m );
		return packages.contains( returnType ) && returnType.isAssignableTo( assignable )
				&& ( !statics || Modifier.isStatic( m.getModifiers() ) )
				&& ( accessible == null || m.isAnnotationPresent( accessible ) );
	}
}
