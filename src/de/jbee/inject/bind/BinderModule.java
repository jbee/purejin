package de.jbee.inject.bind;

import static de.jbee.inject.Source.source;
import static de.jbee.inject.bind.Bootstrap.nonnullThrowsReentranceException;
import de.jbee.inject.Instance;
import de.jbee.inject.Name;
import de.jbee.inject.Scope;
import de.jbee.inject.Type;
import de.jbee.inject.bind.Binder.RootBinder;
import de.jbee.inject.bind.Binder.ScopedBinder;
import de.jbee.inject.bind.Binder.TargetedBinder;
import de.jbee.inject.bind.Binder.TypedBinder;
import de.jbee.inject.bind.Binder.TypedElementBinder;

public abstract class BinderModule
		extends BootstrappingModule
		implements BasicBinder.RootBasicBinder {

	private RootBinder binder;

	@Override
	public final void declare( Bindings bindings ) {
		nonnullThrowsReentranceException( binder );
		this.binder = Binder.create( bindings, source( getClass() ) );
		declare();
	}

	protected abstract void declare();

	@Override
	public ScopedBinder per( Scope scope ) {
		return binder.per( scope );
	}

	public TargetedBinder injectingInto( Class<?> target ) {
		return binder.injectingInto( target );
	}

	@Override
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

	@Override
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

	public <T> TypedBinder<T> superbind( Class<T> type ) {
		return binder.superbind( type );
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
