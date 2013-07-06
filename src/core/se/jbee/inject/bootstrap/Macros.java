/*
 *  Copyright (c) 2012, Jan Bernitt 
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
import static se.jbee.inject.bootstrap.SuppliedBy.parametrizedInstance;
import static se.jbee.inject.util.Constructible.constructible;
import static se.jbee.inject.util.Metaclass.metaclass;
import static se.jbee.inject.util.ToString.describe;

import java.lang.reflect.Constructor;

import se.jbee.inject.Array;
import se.jbee.inject.DeclarationType;
import se.jbee.inject.Instance;
import se.jbee.inject.Parameter;
import se.jbee.inject.Resource;
import se.jbee.inject.Supplier;
import se.jbee.inject.Target;
import se.jbee.inject.Type;
import se.jbee.inject.util.Constructible;
import se.jbee.inject.util.Producible;

/**
 * A immutable collection of {@link Macro}s each bound to a specific type handled.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Macros {

	public static final Macros EMPTY = new Macros( new Class<?>[0], new Macro<?>[0] );

	public static final Module NO_OP = macro();

	public static final Macros DEFAULT = Macros.EMPTY.use( new ExpandMacro() ).use(
			new ConstructorMacro() ).use( new MethodMacro() ).use( new SubstitutionMacro() ).use(
			new ConfigurationMacro() ).use( new ForwardMacro() ).use( new ArrayElementsMacro() );

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

	@SuppressWarnings ( "unchecked" )
	public <T> Macros use( Macro<T> macro ) {
		Class<?> type = Type.supertype( Macro.class, Type.raw( macro.getClass() ) ).parameter( 0 ).getRawType();
		return use( (Class<? super T>) type, macro );
	}

	public <T> Macros use( Class<T> type, Macro<? extends T> macro ) {
		final int index = index( type );
		return new Macros( Array.insert( types, type, index ), Array.insert( macros, macro, index ) );
	}

	public <T, V> Module expand( Binding<T> binding, V value ) {
		return macro( value ).expand( binding, value );
	}

	@SuppressWarnings ( "unchecked" )
	private <V> Macro<V> macro( V value ) {
		final Class<?> type = value.getClass();
		int index = index( type );
		if ( index < 0 ) {
			throw new IllegalArgumentException( type.getCanonicalName() );
		}
		return (Macro<V>) macros[index];
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
				throw new NullPointerException( "Binding has no supplier" );
			}
			return value;
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
			return binding.suppliedBy( PREDEFINED, supplier( (Type) binding.getType(), elements ) );
		}

		@SuppressWarnings ( "unchecked" )
		static <E> Supplier<E> supplier( Type<E[]> array, Parameter<?>[] elements ) {
			return (Supplier<E>) SuppliedBy.elements( array, (Parameter<? extends E>[]) elements );
		}

	}

	//OPEN the below macros could maybe also impl. by the suppliers in SuppliedBy ?

	private static final class ConstructorMacro
			implements Macro<Constructible<?>> {

		ConstructorMacro() {
			// make visible
		}

		@Override
		public <T> Module expand( Binding<T> binding, Constructible<?> constructible ) {
			return binding.suppliedBy( CONSTRUCTOR,
					SuppliedBy.costructor( constructible.typed( binding.getType() ) ) );
		}

	}

	private static final class MethodMacro
			implements Macro<Producible<?>> {

		MethodMacro() {
			// make visible
		}

		@Override
		public <T> Module expand( Binding<T> binding, Producible<?> producible ) {
			return binding.suppliedBy( METHOD,
					SuppliedBy.method( producible.typed( binding.getType() ) ) );
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
					binding.suppliedBy( SUBSTITUTED, Bootstrap.instance( supplier ) );
				}
				return macro( binding.suppliedBy( SUBSTITUTED, SuppliedBy.reference( supplier ) ),
						implicitBindToConstructor( binding, with ) );
			}
			final Type<? extends T> type = t.castTo( binding.getType() );
			final Instance<T> bound = binding.getInstance();
			if ( !bound.getType().equalTo( type )
					|| !with.getName().isApplicableFor( bound.getName() ) ) {
				return macro(
						binding.suppliedBy( SUBSTITUTED, SuppliedBy.instance( with.typed( type ) ) ),
						implicitBindToConstructor( binding, with ) );
			}
			if ( type.isInterface() ) {
				throw new IllegalArgumentException( "Interface type linked in a loop: " + bound
						+ " > " + with );
			}
			return new ConstructorModule<T>( binding, type.getRawType() );
		}

	}

	private static final class ConfigurationMacro
			implements Macro<Configuring<?>> {

		ConfigurationMacro() {
			// make visible
		}

		@Override
		public <T> Module expand( Binding<T> binding, Configuring<?> by ) {
			return binding.suppliedBy( SUBSTITUTED,
					SuppliedBy.configuration( binding.getType(), by ) );
		}

	}

	private static final class ForwardMacro
			implements Macro<Class<?>> {

		ForwardMacro() {
			// make visible
		}

		@Override
		public <T> Module expand( Binding<T> binding, Class<?> to ) {
			return binding.suppliedBy( SUBSTITUTED,
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

		private final Module[] steps;

		MultiModule( Module... steps ) {
			this.steps = steps;
		}

		@Override
		public void declare( Bindings bindings ) {
			for ( int i = 0; i < steps.length; i++ ) {
				steps[i].declare( bindings );
			}
		}

		@Override
		public String toString() {
			return describe( "multi", steps );
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
				bindings.getMacros().expand( binding, constructible( c ) ).declare( bindings );
			}
		}

	}
}
