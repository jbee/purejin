/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.bootstrap.BindingType.LINK;
import static se.jbee.inject.bootstrap.BindingType.METHOD;
import static se.jbee.inject.bootstrap.BindingType.PREDEFINED;
import static se.jbee.inject.bootstrap.Metaclass.metaclass;
import static se.jbee.inject.bootstrap.Supply.parametrizedInstance;

import java.lang.reflect.Constructor;

import se.jbee.inject.Array;
import se.jbee.inject.BindingIsInconsistent;
import se.jbee.inject.DeclarationType;
import se.jbee.inject.Instance;
import se.jbee.inject.Parameter;
import se.jbee.inject.Resource;
import se.jbee.inject.Supplier;
import se.jbee.inject.Target;
import se.jbee.inject.Type;

/**
 * A immutable collection of {@link Macro}s each bound to a specific type handled.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Macros {

	public static final Macro<Binding<?>> EXPAND = new CheckAndAddMacro();
	public static final Macro<Class<?>> PARAMETRIZED_LINK = new TypeParametrizedLinkMacro();
	public static final Macro<Instance<?>> INSTANCE_LINK = new LinkMacro();
	public static final Macro<Parameter<?>[]> ARRAY = new ArrayElementsMacro();
	public static final Macro<BoundConstructor<?>> CONSTRUCTOR = new ConstructorMacro();
	public static final Macro<BoundMethod<?>> FACTORY_METHOD = new MethodMacro();

	public static final Macros EMPTY = new Macros( new Class<?>[0], new Macro<?>[0] );

	public static final Macros DEFAULT = Macros.EMPTY
			.with( EXPAND ).with( CONSTRUCTOR ).with( FACTORY_METHOD )
			.with( INSTANCE_LINK ).with( PARAMETRIZED_LINK ).with( ARRAY );

	private final Class<?>[] types;
	private final Macro<?>[] macros;

	private Macros( Class<?>[] types, Macro<?>[] macros ) {
		super();
		this.types = types;
		this.macros = macros;
	}

	/**
	 * Uses the given {@link Macro} and derives the {@link #with(Class, Macro)} type from its
	 * declaration. This is a utility method that can be used as long as the {@link Macro}
	 * implementation is not generic.
	 * 
	 * @param macro
	 *            No generic macro class (e.g. decorators)
	 * @return A set of {@link Macros} containing the given one for the type derived from its type
	 *         declaration.
	 */
	@SuppressWarnings ( "unchecked" )
	public <T> Macros with( Macro<T> macro ) {
		Class<?> type = Type.supertype( Macro.class, Type.raw( macro.getClass() ) ).parameter( 0 ).getRawType();
		return with( (Class<? super T>) type, macro );
	}

	/**
	 * Uses the given {@link Macro} for the given exact (no super-types!) type of values.
	 * 
	 * @param type
	 *            The type of value that should be passed to the {@link Macro} as value
	 * @param macro
	 *            The {@link Macro} expanding the type of value
	 * @return A set of {@link Macros} containing the given one
	 */
	public <T> Macros with( Class<T> type, Macro<? extends T> macro ) {
		final int index = index( type );
		return new Macros( Array.insert( types, type, index ), Array.insert( macros, macro, index ) );
	}

	/**
	 * A generic version of {@link Macro#expand(Object, Binding, Bindings)} that
	 * uses the matching predefined {@link Macro} for the actual type of the
	 * value and expands it.
	 * 
	 * @param binding
	 *            The usually incomplete binding to expand (and add to
	 *            {@link Bindings})
	 * @param value
	 *            Non-null value to expand via matching {@link Macro}
	 * 
	 * @throws BindingIsInconsistent
	 *             In case no {@link Macro} had been declared for the type of
	 *             value argument
	 */
	public <T, V> void expandInto( Bindings bindings, Binding<T> binding, V value ) {
		macroForValueOf( value.getClass() ).expand( value, binding, bindings );
	}
	
	public <T> void expandInto(Bindings bindings, Binding<T> binding) {
		expandInto(bindings, binding, binding);
	}

	@SuppressWarnings ( "unchecked" )
	private <V> Macro<? super V> macroForValueOf( final Class<? extends V> type ) {
		int index = index( type );
		if ( index < 0 ) {
			throw new BindingIsInconsistent( "No macro for type:" + type.getCanonicalName() );
		}
		return (Macro<? super V>) macros[index];
	}

	private int index( final Class<?> type ) {
		for ( int i = 0; i < types.length; i++ ) {
			if ( types[i] == type ) {
				return i;
			}
		}
		return -1;
	}

	private static final class CheckAndAddMacro
			implements Macro<Binding<?>> {

		CheckAndAddMacro() { /* make visible */ }

		@Override
		public <T> void expand(Binding<?> binding, Binding<T> incomplete, Bindings bindings) {
			if ( !binding.isComplete() ) { // At this point the binding should be complete
				throw new BindingIsInconsistent( "Tried to add an incomplete binding to bindings: "+incomplete );
			}
			bindings.add(binding); 
		}

	}

	private static final class ArrayElementsMacro
			implements Macro<Parameter<?>[]> {

		ArrayElementsMacro() { /* make visible */ }
		
		@Override
		@SuppressWarnings ( { "unchecked", "rawtypes" } )
		public <T> void expand(Parameter<?>[] elements, Binding<T> incomplete, Bindings bindings) {
			bindings.macros.expandInto( bindings, 
					incomplete.complete( PREDEFINED, supplier( (Type) incomplete.type(), elements ) ));
		}

		@SuppressWarnings ( "unchecked" )
		static <E> Supplier<E> supplier( Type<E[]> array, Parameter<?>[] elements ) {
			return (Supplier<E>) Supply.elements( array, (Parameter<? extends E>[]) elements );
		}

	}

	private static final class ConstructorMacro
			implements Macro<BoundConstructor<?>> {

		ConstructorMacro() { /* make visible */ }

		@Override
		public <T> void expand(BoundConstructor<?> constructor, Binding<T> incomplete, Bindings bindings) {
			bindings.macros.expandInto(bindings, 
					incomplete.complete( BindingType.CONSTRUCTOR, Supply.costructor( constructor.typed( incomplete.type() ) ) ));
		}
	}

	private static final class MethodMacro
			implements Macro<BoundMethod<?>> {

		MethodMacro() {	/* make visible */ }
		
		@Override
		public <T> void expand(BoundMethod<?> method, Binding<T> incomplete, Bindings bindings) {
			bindings.macros.expandInto( bindings, 
					incomplete.complete( METHOD, Supply.method( method.typed( incomplete.type() ) ) ));
		}
	}

	private static final class TypeParametrizedLinkMacro
			implements Macro<Class<?>> {

		TypeParametrizedLinkMacro() { /* make visible */ }

		@Override
		public <T> void expand(Class<?> to, Binding<T> incomplete, Bindings bindings) {
			bindings.macros.expandInto(bindings, 
					incomplete.complete( LINK, parametrizedInstance( anyOf( raw( to ).castTo( incomplete.type() ) ) ) ));
		}

	}	
	
	private static final class LinkMacro
			implements Macro<Instance<?>> {

		LinkMacro() { /* make visible */ }

		@Override
		public <T> void expand(Instance<?> linked, Binding<T> binding, Bindings bindings) {
			Type<?> t = linked.type();
			if ( t.isAssignableTo( raw( Supplier.class ) )
					&& !binding.type().isAssignableTo( raw( Supplier.class ) ) ) {
				@SuppressWarnings ( "unchecked" )
				Class<? extends Supplier<? extends T>> supplier = (Class<? extends Supplier<? extends T>>) t.getRawType();
				bindings.macros.expandInto(bindings, binding.complete( LINK, Supply.reference( supplier ) ));
				implicitlyBindToConstructor( binding, linked, bindings );
				return;
			}
			final Type<? extends T> type = t.castTo( binding.type() );
			final Instance<T> bound = binding.resource.instance;
			if ( !bound.type().equalTo( type ) || !linked.name.isCompatibleWith( bound.name ) ) {
				bindings.macros.expandInto( bindings, binding.complete( LINK, Supply.instance( linked.typed( type ) ) ));
				implicitlyBindToConstructor( binding, linked, bindings );
				return;
			}
			if ( type.isInterface() ) {
				throw new BindingIsInconsistent( "Interface type linked in a loop: " + bound	+ " > " + linked );
			}
			bindToInspectedConstructor(bindings, binding, type.getRawType() );
		}

	}

	static <T> void implicitlyBindToConstructor( Binding<?> incomplete, Instance<T> instance, Bindings bindings ) {
		Class<T> impl = instance.type().getRawType();
		if (!metaclass( impl ).undeterminable()) {
			Binding<T> binding = Binding.binding(  new Resource<T>( instance, Target.ANY ),
					BindingType.CONSTRUCTOR, null, incomplete.scope,
					incomplete.source.typed( DeclarationType.IMPLICIT ) );
			bindToInspectedConstructor(bindings, binding, impl );
		}
	}

	static <T> void bindToInspectedConstructor(Bindings bindings, Binding<T> binding, Class<? extends T> implementer) {
		Constructor<? extends T> c = bindings.inspector.constructorFor( implementer );
		if ( c != null ) {
			bindings.macros.expandInto( bindings, binding, BoundConstructor.bind( c ) );
		}
	}
}
