package se.jbee.inject.bind;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Packages;
import se.jbee.inject.Parameter;
import se.jbee.inject.Type;
import se.jbee.inject.util.Inject;
import se.jbee.inject.util.TypeReflector;

/**
 * The basic {@link Inspector} implementations. It allows to chose {@link Constructor}s and
 * {@link Method}s based on the {@link Annotation}s present, the {@link Type}s a method returns or
 * the {@link Packages} they are defined in.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class Inspect
		implements Inspector {

	private static final Parameter<?>[] NO_PARAMETERS = new Parameter<?>[0];

	/**
	 * @return a {@link Inspector} that will result in all methods and a constructor for all given
	 *         implementation classes. The result can be further restricted using any combination of
	 *         the instance methods of {@link Inject}.
	 */
	public static Inspect all() {
		return new Inspect( false, true, true, Packages.ALL, Type.OBJECT, null, null, null );
	}

	/**
	 * @return same as {@link #all()} but result is already restricted to just static methods.
	 */
	public static Inspect allStatic() {
		return new Inspect( true, true, false, Packages.ALL, Type.OBJECT, null, null, null );
	}

	private final boolean statics;
	private final boolean methods;
	private final boolean constructors;
	private final Packages packages;
	private final Type<?> assignable;
	private final Class<? extends Annotation> methodWith;
	private final Class<? extends Annotation> constructorWith;
	private final Class<? extends Annotation> namedby;

	private Inspect( boolean statics, boolean methods, boolean constructors, Packages packages,
			Type<?> assignable, Class<? extends Annotation> methodWith,
			Class<? extends Annotation> constructorWith, Class<? extends Annotation> namedBy ) {
		super();
		this.methods = methods;
		this.constructors = constructors;
		this.statics = statics;
		this.methodWith = methodWith;
		this.constructorWith = constructorWith;
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
		List<Parameter<?>> res = new ArrayList<Parameter<?>>();
		for ( int i = 0; i < annotations.length; i++ ) {
			Name name = TypeReflector.nameFrom( namedby, annotations[i] );
			if ( name != Name.DEFAULT ) {
				res.add( Instance.instance( name, types[i] ) );
			}
		}
		return res.size() == 0
			? NO_PARAMETERS
			: res.toArray( new Parameter<?>[res.size()] );
	}

	@Override
	public Name nameFor( AccessibleObject obj ) {
		return TypeReflector.nameFrom( namedby, obj );
	}

	@Override
	public <T> AccessibleObject[] inspect( Class<T> implementor ) {
		List<AccessibleObject> res = new ArrayList<AccessibleObject>();
		if ( constructors && packages.contains( Type.raw( implementor ) )
				&& Type.raw( implementor ).isAssignableTo( assignable ) ) {
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

	/**
	 * @return a {@link Inspector} restricted to inspect just methods (no constructors).
	 */
	public Inspect methods() {
		return new Inspect( statics, true, false, packages, assignable, methodWith,
				constructorWith, namedby );
	}

	/**
	 * @return a {@link Inspector} restricted to inspect just constructors (no methods).
	 */
	public Inspect constructors() {
		return new Inspect( statics, false, true, packages, assignable, methodWith,
				constructorWith, namedby );
	}

	/**
	 * @param annotation
	 *            An annotation available at runtime
	 * @return a {@link Inspector} restricted to inspect just constructors and/or methods where the
	 *         given annotation is present.
	 */
	public Inspect annotatedWith( Class<? extends Annotation> annotation ) {
		Inspect res = this;
		if ( methods ) {
			res = res.methodsWith( annotation );
		}
		if ( constructors ) {
			res = res.constructorWith( annotation );
		}
		return res;
	}

	/**
	 * @param annotation
	 *            An annotation available at runtime having at least one property of type
	 *            {@link String}.
	 * @return a {@link Inspector} that tries to extract an instance name from the given annotation.
	 */
	public Inspect namedBy( Class<? extends Annotation> annotation ) {
		return new Inspect( statics, methods, constructors, packages, assignable, methodWith,
				constructorWith, annotation );
	}

	/**
	 * @param packages
	 *            The set of {@link Packages} the return type of a method or the class constructed
	 *            by a constructor should be contained in.
	 * @return a {@link Inspector} restricted to inspect just methods having a return type or a
	 *         constructor of a type that is a member of the set given.
	 */
	public Inspect returnTypeIn( Packages packages ) {
		return new Inspect( statics, methods, constructors, packages, assignable, methodWith,
				constructorWith, namedby );
	}

	/**
	 * @param supertype
	 *            any {@link Type}
	 * @return a {@link Inspector} restricted to inspect just methods or constructors that return a
	 *         {@link Type} that is assignable to the given super-type.
	 */
	public Inspect returnTypeAssignableTo( Type<?> supertype ) {
		return new Inspect( statics, methods, constructors, packages, supertype, methodWith,
				constructorWith, namedby );
	}

	private Inspect methodsWith( Class<? extends Annotation> annotation ) {
		return new Inspect( statics, true, constructors, packages, assignable, annotation,
				constructorWith, namedby );
	}

	private Inspect constructorWith( Class<? extends Annotation> annotation ) {
		return new Inspect( statics, methods, true, packages, assignable, methodWith, annotation,
				namedby );
	}
}
