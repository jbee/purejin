package se.jbee.inject.bind;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import se.jbee.inject.Name;
import se.jbee.inject.Packages;
import se.jbee.inject.Parameter;
import se.jbee.inject.Type;
import se.jbee.inject.util.TypeReflector;

/**
 * The basic {@link Inspector} implementations. It allows to chose {@link Constructor}s and
 * {@link Method}s based on the {@link Annotation}s present, the {@link Type}s a method returns or
 * the {@link Packages} they are defined in.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class Inspected
		implements Inspector {

	public static Inspected all() {
		return new Inspected( false, true, true, Packages.ALL, Type.OBJECT, null, null, null );
	}

	public static Inspected onlyStatic() {
		return new Inspected( true, true, false, Packages.ALL, Type.OBJECT, null, null, null );
	}

	private final boolean statics;
	private final boolean methods;
	private final boolean constructors;
	private final Packages packages;
	private final Type<?> assignable;
	private final Class<? extends Annotation> methodWith;
	private final Class<? extends Annotation> constructorWith;
	private final Class<? extends Annotation> namedWith;

	private Inspected( boolean statics, boolean methods, boolean constructors, Packages packages,
			Type<?> assignable, Class<? extends Annotation> methodWith,
			Class<? extends Annotation> constructorWith, Class<? extends Annotation> namedWith ) {
		super();
		this.methods = methods;
		this.constructors = constructors;
		this.statics = statics;
		this.methodWith = methodWith;
		this.constructorWith = constructorWith;
		this.packages = packages;
		this.namedWith = namedWith;
		this.assignable = assignable;
	}

	@Override
	public Parameter<?>[] parametersFor( AccessibleObject obj ) {
		return new Parameter<?>[0];
	}

	@Override
	public Name nameFor( AccessibleObject obj ) {
		return TypeReflector.nameFrom( obj, namedWith );
	}

	@Override
	public <T> AccessibleObject[] inspect( Class<T> implementor ) {
		List<AccessibleObject> res = new ArrayList<AccessibleObject>();
		if ( constructors && packages.contains( Type.raw( implementor ) ) ) {
			if ( constructorWith != null ) {
				for ( Constructor<?> c : implementor.getDeclaredConstructors() ) {
					if ( c.isAnnotationPresent( constructorWith ) ) {
						res.add( c );
					}
				}
			} else {
				res.add( TypeReflector.defaultConstructor( implementor ) );
			}
		}
		if ( methods ) {
			for ( Method m : implementor.getDeclaredMethods() ) {
				if ( TypeReflector.methodMatches( m, statics, assignable, packages, methodWith ) ) {
					res.add( m );
				}
			}
		}
		return res.toArray( new AccessibleObject[res.size()] );
	}

	public Inspected methods() {
		return new Inspected( statics, true, false, packages, assignable, methodWith,
				constructorWith, namedWith );
	}

	public Inspected constructors() {
		return new Inspected( statics, false, true, packages, assignable, methodWith,
				constructorWith, namedWith );
	}

	public Inspected methodsWith( Class<? extends Annotation> annotation ) {
		return new Inspected( statics, true, constructors, packages, assignable, annotation,
				constructorWith, namedWith );
	}

	public Inspected constructorWith( Class<? extends Annotation> annotation ) {
		return new Inspected( statics, methods, true, packages, assignable, methodWith, annotation,
				namedWith );
	}

	public Inspected annotatedWith( Class<? extends Annotation> constructorOrMethodAnnotation ) {
		Inspected res = this;
		if ( methods ) {
			res = res.methodsWith( constructorOrMethodAnnotation );
		}
		if ( constructors ) {
			res = res.constructorWith( constructorOrMethodAnnotation );
		}
		return res;
	}

	public Inspector returningTypeIn( Packages packages ) {
		return new Inspected( statics, methods, constructors, packages, assignable, methodWith,
				constructorWith, namedWith );
	}

	public Inspected namedWith( Class<? extends Annotation> annotation ) {
		return new Inspected( statics, methods, constructors, packages, assignable, methodWith,
				constructorWith, annotation );
	}

	public Inspected assignableTo( Type<?> supertype ) {
		return new Inspected( statics, methods, constructors, packages, supertype, methodWith,
				constructorWith, namedWith );
	}
}
