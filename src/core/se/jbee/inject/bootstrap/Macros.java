package se.jbee.inject.bootstrap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import se.jbee.inject.Instance;
import se.jbee.inject.Parameter;

public interface Macros {

	<T> Module expand( Binding<T> binding );

	<T> Module link( Binding<T> binding, Instance<? extends T> linked );

	<T> Module construct( Binding<T> binding, Constructor<? extends T> constructor,
			Parameter<?>... parameters );

	<T> Module produce( Binding<T> binding, Object instance, Method method,
			Parameter<?>... parameters );

}
