package de.jbee.inject.util;

import static de.jbee.inject.Source.source;
import de.jbee.inject.Binder;
import de.jbee.inject.Module;
import de.jbee.inject.util.RichBinder.RichRootBinder;

public abstract class PackageModule
		implements Module {

	private RichRootBinder root;

	@Override
	public final void configure( Binder binder ) {
		if ( this.root != null ) {
			throw new IllegalStateException( "Reentrance not allowed!" );
		}
		this.root = RichBinder.root( binder, source( getClass() ) );
		configure();
	}

	abstract void configure();
}
