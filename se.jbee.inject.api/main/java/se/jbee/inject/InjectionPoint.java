package se.jbee.inject;

import java.lang.reflect.AnnotatedElement;

public interface InjectionPoint extends Annotated {

	InjectionPoint NONE = () -> Annotated.NOT_ANNOTATED;

	static InjectionPoint at(AnnotatedElement param) {
		return () -> param;
	}

	default boolean isNone() {
		return this == NONE;
	}
}
