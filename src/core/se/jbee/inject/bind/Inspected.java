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
 * Utility class containing common {@link Inspector} implementations.
 * 
 * To 'inspect' means to use reflection or similar techniques to extract information from types.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 */
public class Inspected
		implements Inspector {

	public static Inspected all() {
		return new Inspected( true, true, null, null, Packages.ALL );
	}

	private final boolean methods;
	private final boolean constructors;
	private final Class<? extends Annotation> methodWith;
	private final Class<? extends Annotation> constructorWith;
	private final Packages packages;

	private Inspected( boolean methods, boolean constructors,
			Class<? extends Annotation> methodWith, Class<? extends Annotation> constructorWith,
			Packages packages ) {
		super();
		this.methods = methods;
		this.constructors = constructors;
		this.methodWith = methodWith;
		this.constructorWith = constructorWith;
		this.packages = packages;
	}

	@Override
	public Parameter<?>[] parametersFor( AccessibleObject obj ) {
		return new Parameter<?>[0];
	}

	@Override
	public Name nameFor( AccessibleObject obj ) {
		return Name.DEFAULT;
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
				if ( packages.contains( Type.returnType( m ) ) ) {
					if ( methodWith == null || m.isAnnotationPresent( methodWith ) ) {
						res.add( m );
					}
				}
			}
		}
		return res.toArray( new AccessibleObject[res.size()] );
	}

	public Inspected methods() {
		return new Inspected( true, false, methodWith, constructorWith, packages );
	}

	public Inspected constructors() {
		return new Inspected( false, true, methodWith, constructorWith, packages );
	}

	public Inspected methodsWith( Class<? extends Annotation> annotation ) {
		return new Inspected( true, constructors, annotation, constructorWith, packages );
	}

	public Inspected constructorWith( Class<? extends Annotation> annotation ) {
		return new Inspected( methods, true, methodWith, annotation, packages );
	}

	public Inspector returningTypeIn( Packages packages ) {
		return new Inspected( methods, constructors, methodWith, constructorWith, packages );
	}
}
