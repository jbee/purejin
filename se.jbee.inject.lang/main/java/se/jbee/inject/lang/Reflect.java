package se.jbee.inject.lang;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;

import static se.jbee.inject.lang.Type.raw;

public final class Reflect {

	private Reflect() {
		throw new UnsupportedOperationException("util");
	}

	/**
	 * @param <T> actual object type
	 * @param obj the {@link AccessibleObject} to make accessible
	 * @return the given object made accessible.
	 */
	public static <T extends AccessibleObject> T accessible(T obj) {
		/* J11: if (!obj.canAccess(null)) */
			obj.setAccessible(true);
		return obj;
	}

	private static <T> Constructor<T> noArgsConstructor(Class<T> type) {
		if (type.isInterface() || type.isEnum() || type.isPrimitive())
			throw new IllegalArgumentException("Type is not constructed: " + raw(type).toString());
		try {
			return type.getDeclaredConstructor();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to access no argument constructor for type: " + raw(type), e);
		}
	}

	public static <T> T construct(Constructor<T> target, Object[] args,
			Function<Exception, ? extends RuntimeException> exceptionTransformer) {
		try {
			return target.newInstance(args);
		} catch (Exception e) {
			throw wrap(exceptionTransformer).apply(e);
		}
	}

	public static <T> T construct(Class<T> type, Consumer<Constructor<T>> init,
			Function<Exception, ? extends RuntimeException> exceptionTransformer) {
		Constructor<T> target = noArgsConstructor(type);
		init.accept(target);
		return construct(target, new Object[0], exceptionTransformer);
	}

	public static Object produce(Method target, Object owner, Object[] args,
			Function<Exception, ? extends RuntimeException> exceptionTransformer) {
		try {
			return target.invoke(owner, args);
		} catch (Exception e) {
			throw wrap(exceptionTransformer).apply(e);
		}
	}

	public static Object share(Field target, Object owner,
			Function<Exception, ? extends RuntimeException> exceptionTransformer) {
		try {
			return target.get(owner);
		} catch (Exception e) {
			throw wrap(exceptionTransformer).apply(e);
		}
	}

	private static Function<Exception, ? extends RuntimeException> wrap(
			Function<Exception, ? extends RuntimeException> exceptionTransformer) {
		return e -> {
			if (e instanceof IllegalAccessException) {
				IllegalAccessException extended = new IllegalAccessException(
						e.getMessage() + "\n\tEither make the member accessible by making it public or switch on deep reflection by setting Env.GP_USE_DEEP_REFLECTION property to true before bootstrapping the Injector context");
				extended.setStackTrace(e.getStackTrace());
				extended.initCause(e.getCause());
				e = extended;
			}
			return exceptionTransformer.apply(e);
		};
	}
}
