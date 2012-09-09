package de.jbee.inject;

/**
 * Knows how to *resolve* an instance for a given {@link Dependency}.
 * 
 * The process of resolving might contain fabrication of instances.
 * 
 * A {@link Injector} is immutable (at least from the outside view). Once created it provides a
 * certain set of supported dependencies that can be resolved. All calls to
 * {@link #resolve(Dependency)} always have the same result for the same {@linkplain Dependency}.
 * The only exception to this are scoping effects (expiring and parallel instances).
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Injector {

	/**
	 * @return Resolves the instance appropriate for the given {@link Dependency}. In case no such
	 *         instance is known an exception is thrown. The <code>null</code>-reference will never
	 *         be returned.
	 */
	<T> T resolve( Dependency<T> dependency );
}
