package de.jbee.inject;

/**
 * The binding-builders are just utilities to construct calls to
 * {@link #bind(Resource, Supplier, Scope, Source)}
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface BindDeclarator { //OPEN rename to Bindings ? -- its more or less just a collector for binds

	<T> void bind( Resource<T> resource, Supplier<? extends T> supplier, Scope scope, Source source );
}
