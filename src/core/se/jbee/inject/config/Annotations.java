package se.jbee.inject.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class Annotations { //TODO move to Utils

	public static Method methodReturning(Class<?> returnType,
			Class<? extends Annotation> annotationType) {
		for (Method m : annotationType.getDeclaredMethods()) {
			if (returnType == m.getReturnType())
				return m;
		}
		return null;
	}
}
