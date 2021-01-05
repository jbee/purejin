package se.jbee.inject.config;

import java.lang.reflect.Constructor;

@FunctionalInterface
public interface New {

	<T> T call(Constructor<T> target, Object[] args) throws Exception;

}
