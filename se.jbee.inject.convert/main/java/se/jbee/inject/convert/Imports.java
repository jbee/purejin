package se.jbee.inject.convert;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({ TYPE, METHOD, FIELD, CONSTRUCTOR, ANNOTATION_TYPE, PARAMETER })
public @interface Imports {

	Class<?>[] value();

}
