package de.jbee.silk;

/**
 * The binding-builders are just utilities to construct calls to
 * {@link #bind(Resource, Supplier, Scope, Source)}
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Binder {

	<T> void bind( Resource<T> resource, Supplier<T> supplier, Scope scope, Source source );
}
