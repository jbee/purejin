package se.jbee.inject.config;

import java.lang.reflect.Method;

@FunctionalInterface
public interface Invoke {

	Object call(Method target, Object instance, Object[] args) throws Exception;
}
