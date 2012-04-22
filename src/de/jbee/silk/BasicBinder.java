package de.jbee.silk;

/**
 * The binding-builders are just utilities to construct calls to
 * {@link #bind(Reference, Supplier, Scope)}. So all binds consist of this three elements: A
 * {@link Reference} describing WHEN to inject, a {@link Supplier} delivering the instance injected
 * and a {@link Scope} in which the instance lives.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface BasicBinder {

	//OPEN maybe return the binding created ? 
	<T> void bind( Reference<T> reference, Supplier<T> supplier, Scope scope );
}
