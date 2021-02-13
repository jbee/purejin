/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.Env;
import se.jbee.inject.Hint;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.ValueBinder;
import se.jbee.inject.config.HintsBy;
import se.jbee.lang.Type;

import java.lang.reflect.Constructor;

import static se.jbee.lang.Type.classType;

/**
 * A {@link Constructs} is the {@link ValueBinder} expansion wrapper for {@link
 * Constructor} usages or the container equivalent of a {@code new} statement.
 *
 * @param <T> Type of object created
 */
public final class Constructs<T> extends
		ReflectiveDescriptor<Constructor<?>, T> {

	public static <T> Constructs<? extends T> constructs(Type<T> expectedType,
			Constructor<?> target, Env env, Hint<?>... explicitHints) {
		return constructs(expectedType, target,
				env.property(HintsBy.class), explicitHints);
	}

	private static <T> Constructs<? extends T> constructs(Type<T> expectedType,
			Constructor<?> target, HintsBy strategy, Hint<?>... explicitHints) {
		checkBasicCompatibility(classType(target.getDeclaringClass()),
				expectedType, target);
		Hint<T> as = Hint.relativeReferenceTo(expectedType);
		@SuppressWarnings("unchecked")
		Type<? extends T> actualType = (Type<? extends T>) actualType(as, target);
		return new Constructs<>(expectedType, actualType, as, target,
				strategy, explicitHints);
	}

	private Constructs(Type<? super T> expectedType, Type<T> actualType,
			Hint<?> as, Constructor<?> target, HintsBy strategy,
			Hint<?>[] explicitHints) {
		super(expectedType, actualType, as, target, strategy, explicitHints);
		checkConsistentExplicitHints(target.getParameters());
	}

	private static Type<?> actualType(Hint<?> as, Constructor<?> target) {
		return requiresActualReturnType(target, Constructor::getDeclaringClass,
				Constructor::getAnnotatedReturnType) //
				? Type.actualReturnType(target, actualDeclaringType(as, target)) //
				: classType(target.getDeclaringClass());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> Constructs<E> typed(Type<E> supertype) {
		type().castTo(supertype);
		return (Constructs<E>) this;
	}

	public boolean isGeneric() {
		return target.getDeclaringClass().getTypeParameters().length > 0;
	}
	public Hint<?>[] actualParameters(Type<?> actualType, Injector context) {
		return strategy.applyTo(context, target, actualType, explicitHints);
	}
}
