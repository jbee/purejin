package de.jbee.inject.bind;


public interface Linker<T> {

	T[] link( ConstructionStrategy strategy, Module... modules );
}
