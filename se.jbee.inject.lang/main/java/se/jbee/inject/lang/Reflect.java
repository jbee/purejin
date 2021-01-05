package se.jbee.inject.lang;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.function.Function;

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

	public static Object access(Field target, Object instance,
			Function<Exception, ? extends RuntimeException> exceptionTransformer) {
		try {
			return target.get(instance);
		} catch (Exception e) {
			throw wrap(exceptionTransformer).apply(e);
		}
	}

	private static Function<Exception, ? extends RuntimeException> wrap(
			Function<Exception, ? extends RuntimeException> exceptionTransformer) {
		return e -> {
			if (e instanceof IllegalAccessException) {
				IllegalAccessException extended = new IllegalAccessException(
						e.getMessage() + "\n\tEither make the member accessible by making it public or switch on deep reflection by setting Env.USE_DEEP_REFLECTION property to true before bootstrapping the Injector context");
				extended.setStackTrace(e.getStackTrace());
				extended.initCause(e.getCause());
				e = extended;
			}
			return exceptionTransformer.apply(e);
		};
	}
}
