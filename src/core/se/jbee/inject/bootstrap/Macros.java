/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.bootstrap.BindingType.CONSTRUCTOR;
import static se.jbee.inject.bootstrap.BindingType.METHOD;
import static se.jbee.inject.bootstrap.BindingType.PREDEFINED;
import static se.jbee.inject.bootstrap.BindingType.SUBSTITUTED;
import static se.jbee.inject.bootstrap.Supply.parametrizedInstance;
import static se.jbee.inject.util.Metaclass.metaclass;

import java.lang.reflect.Constructor;

import se.jbee.inject.Array;
import se.jbee.inject.DIRuntimeException;
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

	public static final Macro<Binding<?>> EXPAND = new ExpandMacro();
	public static final Macro<Class<?>> FORWARD = new ForwardMacro();
	public static final Macro<Instance<?>> SUBSTITUTE = new SubstitutionMacro();
	public static final Macro<Parameter<?>[]> ARRAY = new ArrayElementsMacro();
	public static final Macro<BoundConstructor<?>> CONSTRUCT = new ConstructorMacro();
	public static final Macro<BoundMethod<?>> PRODUCE = new MethodMacro();

	public static final Module NO_OP = macro();

	public static final Macros EMPTY = new Macros( new Class<?>[0], new Macro<?>[0] );

	public static final Macros DEFAULT = Macros.EMPTY
			.use( EXPAND ).use( CONSTRUCT ).use( PRODUCE )
			.use( SUBSTITUTE ).use( FORWARD ).use( ARRAY );

	public static Module macro( Module mandatory, Module optional ) {
		return optional == null || optional == NO_OP
			? mandatory
			: new MultiModule( mandatory, optional );
	}

	public static Module macro( Module... steps ) {
		return new MultiModule( steps );
	}

	private final Class<?>[] types;
	private final Macro<?>[] macros;

	private Macros( Class<?>[] types, Macro<?>[] macros ) {
		super();
		this.types = types;
		this.macros = macros;
	}

	/**
	 * Uses the given {@link Macro} and derives the {@link #use(Class, Macro)} type from its
	 * declaration. This is a utility method that can be used as long as the {@link Macro}
	 * implementation is not generic.
	 * 
	 * @param macro
	 *            No generic macro class (e.g. decorators)
	 * @return A set of {@link Macros} containing the given one for the type derived from its type
	 *         declaration.
	 */
	@SuppressWarnings ( "unchecked" )
	public <T> Macros use( Macro<T> macro ) {
		Class<?> type = Type.supertype( Macro.class, Type.raw( macro.getClass() ) ).parameter( 0 ).getRawType();
		return use( (Class<? super T>) type, macro );
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
	public <T> Macros use( Class<T> type, Macro<? extends T> macro ) {
		final int index = index( type );
		return new Macros( Array.insert( types, type, index ), Array.insert( macros, macro, index ) );
	}

	/**
	 * A generic version of {@link Macro#expand(Binding, Object)} that uses the matching predefined
	 * {@link Macro} for the actual type of the value and expands it.
	 * 
	 * @param binding
	 *            The incomplete binding to expand
	 * @param value
	 *            Non-null value to expand via matching {@link Macro}
	 * @return {@link Module} results from {@link Macro#expand(Binding, Object)}
	 * @throws DIRuntimeException
	 *             In case no {@link Macro} had been declared for the type of value argument
	 */
	public <T, V> Module expand( Binding<T> binding, V value ) {
		return macroForValueOf( value.getClass() ).expand( binding, value );
	}

	@SuppressWarnings ( "unchecked" )
	private <V> Macro<? super V> macroForValueOf( final Class<? extends V> type ) {
		int index = index( type );
		if ( index < 0 ) {
			throw new DIRuntimeException( "No macro for type:" + type.getCanonicalName() );
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

	private static final class ExpandMacro
			implements Macro<Binding<?>> {

		ExpandMacro() {
			// make visible
		}

		@Override
		public <T> Module expand( Binding<T> binding, Binding<?> value ) {
			if ( value.supplier == null ) {
				// At this point the binding should be complete
				throw new NullPointerException( "Binding has no supplier" );
			}
			return value; // the macro expansion ends here - a complete bind has been created 
		}

	}

	private static final class ArrayElementsMacro
			implements Macro<Parameter<?>[]> {

		ArrayElementsMacro() {
			// make visible
		}

		@Override
		@SuppressWarnings ( { "unchecked", "rawtypes" } )
		public <T> Module expand( Binding<T> binding, Parameter<?>[] elements ) {
			return binding.complete( PREDEFINED, supplier( (Type) binding.getType(), elements ) );
		}

		@SuppressWarnings ( "unchecked" )
		static <E> Supplier<E> supplier( Type<E[]> array, Parameter<?>[] elements ) {
			return (Supplier<E>) Supply.elements( array, (Parameter<? extends E>[]) elements );
		}

	}

	private static final class ConstructorMacro
			implements Macro<BoundConstructor<?>> {

		ConstructorMacro() {
			// make visible
		}

		@Override
		public <T> Module expand( Binding<T> binding, BoundConstructor<?> constructor ) {
			return binding.complete( CONSTRUCTOR,
					Supply.costructor( constructor.typed( binding.getType() ) ) );
		}

	}

	private static final class MethodMacro
			implements Macro<BoundMethod<?>> {

		MethodMacro() {
			// make visible
		}

		@Override
		public <T> Module expand( Binding<T> binding, BoundMethod<?> method ) {
			return binding.complete( METHOD,
					Supply.method( method.typed( binding.getType() ) ) );
		}

	}

	private static final class SubstitutionMacro
			implements Macro<Instance<?>> {

		SubstitutionMacro() {
			// make visible
		}

		@Override
		public <T> Module expand( Binding<T> binding, Instance<?> with ) {
			Type<?> t = with.getType();
			if ( t.isAssignableTo( raw( Supplier.class ) )
					&& !binding.getType().isAssignableTo( raw( Supplier.class ) ) ) {
				@SuppressWarnings ( "unchecked" )
				Class<? extends Supplier<? extends T>> supplier = (Class<? extends Supplier<? extends T>>) t.getRawType();
				if ( t.isFinal() && metaclass( t.getRawType() ).monomodal() ) {
					binding.complete( SUBSTITUTED, Bootstrap.instance( supplier ) );
				}
				return macro( 
						binding.complete( SUBSTITUTED, Supply.reference( supplier ) ),
						implicitBindToConstructor( binding, with ) );
			}
			final Type<? extends T> type = t.castTo( binding.getType() );
			final Instance<T> bound = binding.getInstance();
			if ( !bound.getType().equalTo( type )
					|| !with.name.isApplicableFor( bound.name ) ) {
				return macro(
						binding.complete( SUBSTITUTED, Supply.instance( with.typed( type ) ) ),
						implicitBindToConstructor( binding, with ) );
			}
			if ( type.isInterface() ) {
				throw new IllegalArgumentException( "Interface type linked in a loop: " + bound
						+ " > " + with );
			}
			return new ConstructorModule<T>( binding, type.getRawType() );
		}

	}

	private static final class ForwardMacro
			implements Macro<Class<?>> {

		ForwardMacro() {
			// make visible
		}

		@Override
		public <T> Module expand( Binding<T> binding, Class<?> to ) {
			return binding.complete( SUBSTITUTED,
					parametrizedInstance( anyOf( raw( to ).castTo( binding.getType() ) ) ) );
		}

	}

	static <T> Module implicitBindToConstructor( Binding<?> binding, Instance<T> instance ) {
		Class<T> impl = instance.getType().getRawType();
		return metaclass( impl ).undeterminable()
			? null
			: new ConstructorModule<T>( Binding.binding( new Resource<T>( instance, Target.ANY ),
					BindingType.CONSTRUCTOR, null, binding.scope,
					binding.source.typed( DeclarationType.IMPLICIT ) ), impl );
	}

	/**
	 * A composite to combine several {@link Module}s into one.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	private static final class MultiModule
			implements Module {

		private final Module[] parts;

		MultiModule( Module... parts ) {
			this.parts = parts;
		}

		@Override
		public void declare( Bindings bindings ) {
			for ( int i = 0; i < parts.length; i++ ) {
				parts[i].declare( bindings );
			}
		}

		@Override
		public String toString() {
			return Supply.describe( "multi", parts );
		}
	}

	/**
	 * Binds to inspected constructor if the given implementer {@link Class}.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 */
	private static final class ConstructorModule<T>
			implements Module {

		private final Binding<T> binding;
		private final Class<? extends T> implementer;

		ConstructorModule( Binding<T> binding, Class<? extends T> implementer ) {
			this.binding = binding;
			this.implementer = implementer;
		}

		@Override
		public void declare( Bindings bindings ) {
			Constructor<? extends T> c = bindings.getInspector().constructorFor( implementer );
			if ( c != null ) {
				bindings.getMacros().expand( binding, BoundConstructor.bind( c ) ).declare( bindings );
			}
		}

	}
}
