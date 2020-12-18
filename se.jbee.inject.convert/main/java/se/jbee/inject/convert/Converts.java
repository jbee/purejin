package se.jbee.inject.convert;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Repeatable(ConvertsMultiple.class)
@Retention(RUNTIME)
@Target({ TYPE, METHOD, CONSTRUCTOR, FIELD })
public @interface Converts {

	String[] value();

	Class<?>[] imports() default {};
}
