package se.jbee.inject.config;

import java.lang.reflect.Field;

@FunctionalInterface
public interface Get {

	Object call(Field target, Object instance) throws Exception;
}
