package se.jbee.inject.binder;

import se.jbee.inject.*;
import se.jbee.inject.lang.Reflect;
import se.jbee.inject.lang.Type;

import java.lang.reflect.Field;

/**
 * Accesses a value that is extracted from a {@link Field}. The {@link Field} is
 * read each time the {@link Supplier} supplying is asked to supply the value.
 * In other words it depends on {@link se.jbee.inject.Scope}.
 *
 * @param <T> type of the constant shared
 * @since 8.1
 */
public final class Accesses<T> extends ReflectiveDescriptor<Field, T> implements Supplier<T> {

	public static <T> Accesses<? extends T> accesses(Type<T> expectedType,
			Object as, Field target) {
		@SuppressWarnings("unchecked")
		Type<? extends T> actualType = (Type<? extends T>) actualType(as, target);
		return new Accesses<>(expectedType, actualType, as, target);
	}

	public static <T> Accesses<?> accesses(Object as, Field target) {
		@SuppressWarnings("unchecked")
		Type<T> actualType = (Type<T>) actualType(as, target);
		return new Accesses<>(actualType, actualType, as, target);
	}

	private Accesses(Type<? super T> expectedType, Type<T> actualType,
			Object as, Field target) {
		super(expectedType, as, target, Hint.none(), actualType);
	}

	private static Type<?> actualType(Object owner, Field target) {
		return requiresActualReturnType(target, Field::getType,
				Field::getAnnotatedType) //
				? Type.actualFieldType(target, actualDeclaringType(owner, target)) //
				: Type.fieldType(target);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> Accesses<E> typed(Type<E> supertype) {
		type().castTo(supertype); // make sure is valid
		return (Accesses<E>) this;
	}

	/**
	 * This can be thought of as the default implementation.
	 * <p>
	 * So an {@link Accesses} can be used as the {@link Supplier} but it can
	 * also just be used as the {@link Descriptor} and data to create another {@link
	 * Supplier}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T supply(Dependency<? super T> dep, Injector context) {
		return (T) Reflect.access(target, as,
				e -> UnresolvableDependency.SupplyFailed.valueOf(e, target));
	}
}
