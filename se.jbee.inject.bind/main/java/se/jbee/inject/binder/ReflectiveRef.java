package se.jbee.inject.binder;

import se.jbee.inject.Annotated;
import se.jbee.inject.Hint;
import se.jbee.inject.Ref;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Typed;

import java.lang.reflect.*;
import java.util.function.Function;

abstract class ReflectiveRef<M extends AnnotatedElement & Member, T>
		implements Typed<T>, Annotated, Ref {

	public final Type<? super T> expectedType;
	public final Type<T> actualType;
	public final Object as;
	public final M target;
	public final Hint<?>[] hints;

	public ReflectiveRef(Type<? super T> expectedType, Object as, M target, Hint<?>[] hints,
			Type<T> actualType) {
		actualType.castTo(expectedType);
		this.expectedType = expectedType;
		this.actualType = actualType;
		this.as = as;
		this.target = target;
		this.hints = hints;
	}

	@Override
	public final AnnotatedElement element() {
		return target;
	}

	static <T extends Member> boolean requiresActualReturnType(T member,
			Function<T, Class<?>> type,
			Function<T, AnnotatedType> genericType) {
		return !Modifier.isStatic(member.getModifiers())
				&& member.getDeclaringClass().getTypeParameters().length > 0
				&& !type.apply(member).equals(genericType.apply(member).getType());
	}

	static Type<?> actualDeclaringType(Object as, Member target) {
		return as instanceof Hint
				? ((Hint<?>) as).asType
				: Type.classType(target.getDeclaringClass());
	}

	static <T> void checkBasicCompatibility(Type<?> memberType,
			Type<T> expectedType, Member target) {
		if (memberType.equalTo(Type.WILDCARD))
			return; // it could be any type - fine
		if (memberType.isAssignableTo(expectedType))
			return; // a supertype of what we have - fine
		if (!expectedType.isAssignableTo(memberType))
			throw new IllegalArgumentException(
					"Expected " + expectedType + " but the member " + target + " provides type " + memberType);
	}

	public boolean isStatic() {
		return Modifier.isStatic(target.getModifiers());
	}

	@Override
	public final Type<T> type() {
		return actualType;
	}

	/**
	 * @return The actual {@link Method#getDeclaringClass()} when considering
	 * potential type arguments for type parameters as specified by the {@link
	 * Hint} given for the owner.
	 *
	 * @see #isHinted()
	 */
	public final Type<?> actualDeclaringType() {
		return actualDeclaringType(as, target);
	}

	/**
	 * @return By convention if the given owner is a {@link Hint} object this is
	 * not considered the actual target object but as a reference to resolve the
	 * actual target. This is specifically useful when the target is a type with
	 * type parameters as there is no other way to know for what actual type
	 * parameters the instance was build for then to resolve it for a specific
	 * set of type parameters.
	 */
	public final boolean isHinted() {
		return as instanceof Hint;
	}

	public Hint<?> getAsHint() {
		return isHinted() ? (Hint<?>) as : null;
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "[" + expectedType + "] <= " + target.getClass().getSimpleName() + "[" + actualType + "]";
	}
}
