package de.jbee.inject;

public interface InjectionPoint<T> {

	void inject( T instance );
}
