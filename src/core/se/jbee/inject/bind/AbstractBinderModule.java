/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Source.source;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.bind.Configured.configured;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Packages;
import se.jbee.inject.Parameter;
import se.jbee.inject.Scope;
import se.jbee.inject.Type;
import se.jbee.inject.bind.Binder.InspectBinder;
import se.jbee.inject.bind.Binder.RootBinder;
import se.jbee.inject.bind.Binder.ScopedBinder;
import se.jbee.inject.bind.Binder.TargetedBinder;
import se.jbee.inject.bind.Binder.TypedBinder;
import se.jbee.inject.bind.Binder.TypedElementBinder;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Inspect;
import se.jbee.inject.bootstrap.Inspector;
import se.jbee.inject.util.Scoped;

public abstract class AbstractBinderModule {

	private RootBinder binder;

	protected AbstractBinderModule() {
		this( Scoped.APPLICATION );
	}

	protected AbstractBinderModule( Scope inital ) {
		this.binder = Binder.create( null, Inspect.DEFAULT, source( getClass() ), inital );
	}

	protected void init( Bindings bindings, Inspector inspector ) {
		Bootstrap.nonnullThrowsReentranceException( binder.bindings );
		this.binder = binder.into( bindings ).using( inspector ).with( source( getClass() ) );
	}

	//TODO also add provide with an instance

	public <T> void provide( Class<T> implementation, Parameter<?>... parameters ) {
		binder.provide( implementation, parameters );
	}

	public <T> void require( Class<T> dependency ) {
		binder.require( dependency );
	}

	public <T> void require( Type<T> dependency ) {
		binder.require( dependency );
	}

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

	public Binder in( Packages packages ) {
		return binder.in( packages );
	}

	public <T> TypedBinder<T> bind( Class<T> type ) {
		return binder.bind( type );
	}

	public <E> TypedElementBinder<E> arraybind( Class<E[]> type ) {
		return binder.arraybind( type );
	}

	public InspectBinder bind( Inspector inspector ) {
		return binder.bind( inspector );
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

	public <T, C extends Enum<C>> TypedBinder<T> configbind( C configured, Class<T> type ) {
		return configbind(
				configured( instance( Name.DEFAULT, raw( configured.getDeclaringClass() ) ) ),
				configured, type );
	}

	public <T, C> TypedBinder<T> configbind( Configured<C> configured, C value, Class<T> type ) {
		return configbind( configured, value, raw( type ) );
	}

	public <T, C> TypedBinder<T> configbind( Configured<C> configured, C value, Type<T> type ) {
		return binder.configbind( configured, value, type );
	}

}