/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import java.util.EnumSet;
import java.util.IdentityHashMap;

/**
 * {@link Options} are used to model configurations of the bootstrapping process through one enum
 * for each configurable property (each property is identified by the enum's {@link Class} object).
 * 
 * Each property can be used as a set or single associate value. So a option property can describe
 * either alternatives where one should be chosen or options with multiple choice. It is up to the
 * author of the module to decide and use correctly.
 * 
 * {@linkplain Options} are immutable! Use {@link #chosen(Enum)} to build up sets.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Options {

	public static final Options STANDARD = new Options(
			new IdentityHashMap<Class<? extends Enum<?>>, EnumSet<?>>() );

	private final IdentityHashMap<Class<? extends Enum<?>>, EnumSet<?>> properties;

	private Options( IdentityHashMap<Class<? extends Enum<?>>, EnumSet<?>> properties ) {
		this.properties = properties;
	}

	public <C extends Enum<C>> boolean isChosen( Class<C> property, C option ) {
		EnumSet<?> options = properties.get( property );
		return options == null || options.isEmpty()
			? ( option == null )
			: options.contains( option );
	}

	public <C extends Enum<C>> Options chosen( C option ) {
		if ( option == null ) {
			return this;
		}
		return with( option.getDeclaringClass(), EnumSet.of( option ) );
	}

	private <C extends Enum<C>> Options with( Class<C> property, EnumSet<C> options ) {
		IdentityHashMap<Class<? extends Enum<?>>, EnumSet<?>> clone = copy();
		clone.put( property, options );
		return new Options( clone );
	}

	public final <C extends Enum<C>> Options chosen( C... options ) {
		if ( options.length == 0 ) {
			return this;
		}
		return with( options[0].getDeclaringClass(), EnumSet.of( options[0], options ) );
	}

	@SuppressWarnings ( "unchecked" )
	private IdentityHashMap<Class<? extends Enum<?>>, EnumSet<?>> copy() {
		return (IdentityHashMap<Class<? extends Enum<?>>, EnumSet<?>>) properties.clone();
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Options && properties.equals( ( (Options) obj ).properties );
	}

	@Override
	public int hashCode() {
		return properties.hashCode();
	}

	@Override
	public String toString() {
		return properties.toString();
	}
}
