package de.jbee.inject.util;

import static de.jbee.inject.Source.source;
import de.jbee.inject.Binder;
import de.jbee.inject.Instance;
import de.jbee.inject.Module;
import de.jbee.inject.Name;
import de.jbee.inject.Scope;
import de.jbee.inject.Type;
import de.jbee.inject.util.RichBinder.RichBasicBinder;
import de.jbee.inject.util.RichBinder.RichRootBinder;
import de.jbee.inject.util.RichBinder.RichScopedBinder;
import de.jbee.inject.util.RichBinder.RichTargetedBinder;
import de.jbee.inject.util.RichBinder.RichTypedBinder;

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

	protected abstract void configure();

	public void install( Module module ) {
		module.configure( root.binder() );
	}

	public void extend( Class<? extends Module> dependency ) {
		//OPEN how to keep track of the installed modules transparent ? 
	}

	public RichScopedBinder in( Scope scope ) {
		return root.in( scope );
	}

	public RichTargetedBinder injectingInto( Class<?> target ) {
		return root.injectingInto( target );
	}

	public RichTargetedBinder injectingInto( Instance<?> target ) {
		return root.injectingInto( target );
	}

	public RichBasicBinder inPackageOf( Class<?> packageOf ) {
		return root.inPackageOf( packageOf );
	}

	public <T> RichTypedBinder<T> bind( Class<T> type ) {
		return root.bind( type );
	}

	public <T> RichTypedBinder<T> bind( Instance<T> instance ) {
		return root.bind( instance );
	}

	public <T> RichTypedBinder<T> bind( Name name, Class<T> type ) {
		return root.bind( name, type );
	}

	public <T> RichTypedBinder<T> bind( Name name, Type<T> type ) {
		return root.bind( name, type );
	}

	public <T> RichTypedBinder<T> bind( Type<T> type ) {
		return root.bind( type );
	}

	public <T> RichTypedBinder<T> autobind( Type<T> type ) {
		return root.autobind( type );
	}

	public <T> RichTypedBinder<T> autobind( Class<T> type ) {
		return root.autobind( type );
	}

	public <T> RichTypedBinder<T> multibind( Instance<T> instance ) {
		return root.multibind( instance );
	}

	public <T> RichTypedBinder<T> multibind( Type<T> type ) {
		return root.multibind( type );
	}

	public <T> RichTypedBinder<T> multibind( Class<T> type ) {
		return root.multibind( type );
	}

	public <T> RichTypedBinder<T> multibind( Name name, Class<T> type ) {
		return root.multibind( name, type );
	}

	public <T> RichTypedBinder<T> multibind( Name name, Type<T> type ) {
		return root.multibind( name, type );
	}
}
