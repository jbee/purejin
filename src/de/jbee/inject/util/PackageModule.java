package de.jbee.inject.util;

import static de.jbee.inject.Source.source;
import de.jbee.inject.Bindings;
import de.jbee.inject.Instance;
import de.jbee.inject.Name;
import de.jbee.inject.Scope;
import de.jbee.inject.Type;
import de.jbee.inject.util.Binder.RootBinder;
import de.jbee.inject.util.Binder.ScopedBinder;
import de.jbee.inject.util.Binder.TargetedBinder;
import de.jbee.inject.util.Binder.TypedBinder;
import de.jbee.inject.util.Binder.TypedElementBinder;

public abstract class PackageModule
		extends BootstrappingModule {

	private RootBinder binder;

	@Override
	public final void configure( Bindings bindings ) {
		if ( this.binder != null ) {
			throw new IllegalStateException( "Reentrance not allowed!" );
		}
		this.binder = Binder.create( bindings, source( getClass() ) );
		configure();
	}

	protected abstract void configure();

	public ScopedBinder in( Scope scope ) {
		return binder.in( scope );
	}

	public TargetedBinder injectingInto( Class<?> target ) {
		return binder.injectingInto( target );
	}

	public TargetedBinder injectingInto( Instance<?> target ) {
		return binder.injectingInto( target );
	}

	public Binder inPackageOf( Class<?> packageOf ) {
		return binder.inPackageOf( packageOf );
	}

	public <T> TypedBinder<T> bind( Class<T> type ) {
		return binder.bind( type );
	}

	public <E> TypedElementBinder<E> bind( Class<E[]> type ) {
		return binder.bind( type );
	}

	public <T> TypedBinder<T> bind( Instance<T> instance ) {
		return binder.bind( instance );
	}

	public <T> TypedBinder<T> bind( Name name, Class<T> type ) {
		return binder.bind( name, type );
	}

	public <T> TypedBinder<T> bind( Name name, Type<T> type ) {
		return binder.bind( name, type );
	}

	public <T> TypedBinder<T> bind( Type<T> type ) {
		return binder.bind( type );
	}

	public <T> TypedBinder<T> autobind( Type<T> type ) {
		return binder.autobind( type );
	}

	public <T> TypedBinder<T> autobind( Class<T> type ) {
		return binder.autobind( type );
	}

	public <T> TypedBinder<T> multibind( Instance<T> instance ) {
		return binder.multibind( instance );
	}

	public <T> TypedBinder<T> multibind( Type<T> type ) {
		return binder.multibind( type );
	}

	public <T> TypedBinder<T> multibind( Class<T> type ) {
		return binder.multibind( type );
	}

	public <T> TypedBinder<T> multibind( Name name, Class<T> type ) {
		return binder.multibind( name, type );
	}

	public <T> TypedBinder<T> multibind( Name name, Type<T> type ) {
		return binder.multibind( name, type );
	}
}
