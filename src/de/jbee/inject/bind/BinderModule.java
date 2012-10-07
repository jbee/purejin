package de.jbee.inject.bind;

import static de.jbee.inject.Source.source;
import de.jbee.inject.Instance;
import de.jbee.inject.Name;
import de.jbee.inject.Packages;
import de.jbee.inject.Scope;
import de.jbee.inject.Type;
import de.jbee.inject.bind.Binder.RootBinder;
import de.jbee.inject.bind.Binder.ScopedBinder;
import de.jbee.inject.bind.Binder.TargetedBinder;
import de.jbee.inject.bind.Binder.TypedBinder;
import de.jbee.inject.bind.Binder.TypedElementBinder;
import de.jbee.inject.util.Scoped;

public abstract class BinderModule
		extends BootstrappingModule
		implements BasicBinder.RootBasicBinder {

	private RootBinder binder;

	protected BinderModule() {
		this( Scoped.APPLICATION );
	}

	protected BinderModule( Scope inital ) {
		this.binder = Binder.create( null, Link.DEFAULE_CONSTRUCTION_STRATEGY,
				source( BinderModule.class ), inital );
	}

	@Override
	public void declare( Bindings bindings, ConstructionStrategy strategy ) {
		BootstrappingModule.nonnullThrowsReentranceException( binder.bindings );
		this.binder = binder.into( bindings ).using( strategy ).with( source( getClass() ) );
		declare();
	}

	protected abstract void declare();

	@Override
	public ScopedBinder per( Scope scope ) {
		return binder.per( scope );
	}

	public RootBinder asDefault() {
		return binder.asDefault();
	}

	public TargetedBinder injectingInto( Class<?> target ) {
		return binder.injectingInto( target );
	}

	public TargetedBinder injectingInto( Type<?> target ) {
		return binder.injectingInto( target );
	}

	public TargetedBinder injectingInto( Name name, Class<?> type ) {
		return binder.injectingInto( name, type );
	}

	public TargetedBinder injectingInto( Name name, Type<?> type ) {
		return binder.injectingInto( name, type );
	}

	@Override
	public TargetedBinder injectingInto( Instance<?> target ) {
		return binder.injectingInto( target );
	}

	public Binder inPackageOf( Class<?> type ) {
		return binder.inPackageOf( type );
	}

	public Binder inSubPackagesOf( Class<?> type ) {
		return binder.inSubPackagesOf( type );
	}

	public Binder inPackageAndSubPackagesOf( Class<?> type ) {
		return binder.inPackageAndSubPackagesOf( type );
	}

	@Override
	public BasicBinder in( Packages packages ) {
		return binder.in( packages );
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

	public void construct( Class<?> type ) {
		binder.construct( type );
	}

	public void construct( Name name, Class<?> type ) {
		binder.construct( name, type );
	}

	public void construct( Instance<?> instance ) {
		binder.construct( instance );
	}

	public <T> TypedBinder<T> starbind( Class<T> type ) {
		return binder.starbind( type );
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

	public <E extends Enum<E> & Extension<E, ? super T>, T> void extend( Class<E> extension,
			Class<? extends T> type ) {
		binder.extend( extension, type );
	}

	public <E extends Enum<E> & Extension<E, ? super T>, T> void extend( E extension,
			Class<? extends T> type ) {
		binder.extend( extension, type );
	}
}
