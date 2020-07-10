package se.jbee.inject.container;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface Eager {

	//idea for an per class control of eager singletons
	// if the class is annotated the annotation will be bound
	// which allows to resolve all types annotated with @Eager
	// issue is that this is the class implementing a Resource - trying to resolve it might cause a lookup error
	// how to find the Resource of it?
}
