package se.jbee.inject.convert;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Repeatable(ConvertsMultiple.class)
@Retention(RUNTIME)
@Target({ TYPE, METHOD, CONSTRUCTOR, FIELD })
public @interface Converts {

	String[] value();

	Class<?>[] imports() default {};
}
