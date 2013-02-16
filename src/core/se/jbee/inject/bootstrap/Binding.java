package se.jbee.inject.bootstrap;

import se.jbee.inject.Precision;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Source;
import se.jbee.inject.Supplier;

/**
 * Default data strature to represent a 4-tuple created from {@link Bindings}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T>
 *            The type of the bound value (instance)
 */
public final class Binding<T>
		implements Comparable<Binding<?>> {

	public final Resource<T> resource;
	public final Supplier<? extends T> supplier;
	public final Scope scope;
	public final Source source;

	Binding( Resource<T> resource, Supplier<? extends T> supplier, Scope scope, Source source ) {
		super();
		this.resource = resource;
		this.supplier = supplier;
		this.scope = scope;
		this.source = source;
	}

	@Override
	public int compareTo( Binding<?> other ) {
		int res = resource.getType().getRawType().getCanonicalName().compareTo(
				other.resource.getType().getRawType().getCanonicalName() );
		if ( res != 0 ) {
			return res;
		}
		res = Precision.comparePrecision( resource.getInstance(), other.resource.getInstance() );
		if ( res != 0 ) {
			return res;
		}
		res = Precision.comparePrecision( resource.getTarget(), other.resource.getTarget() );
		if ( res != 0 ) {
			return res;
		}
		res = Precision.comparePrecision( source, other.source );
		if ( res != 0 ) {
			return res;
		}
		return -1; // keep order
	}

	@Override
	public String toString() {
		return resource + " / " + scope + " / " + source;
	}

}