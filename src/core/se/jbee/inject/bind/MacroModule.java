/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.bind.SuppliedBy.parametrizedInstance;
import static se.jbee.inject.bootstrap.BindingType.CONSTRUCTOR;
import static se.jbee.inject.bootstrap.BindingType.METHOD;
import static se.jbee.inject.bootstrap.BindingType.SUBSTITUTED;
import static se.jbee.inject.util.Metaclass.metaclass;
import static se.jbee.inject.util.ToString.describe;

import java.lang.reflect.Constructor;

import se.jbee.inject.Instance;
import se.jbee.inject.Source;
import se.jbee.inject.Target;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Binding;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Macro;
import se.jbee.inject.bootstrap.Macros;
import se.jbee.inject.bootstrap.Module;
import se.jbee.inject.util.Constructible;
import se.jbee.inject.util.Producible;

public final class MacroModule
		implements Module {

	public static final Module NO_OP = macro();
	@SuppressWarnings ( "unchecked" )
	public static final Macros MACROS = Macros.macros( ExpandMacro.class, ConstructorMacro.class,
			MethodMacro.class, SubstitutionMacro.class, ConfigurationMacro.class,
			ForwardMacro.class );

	public static Module macro( Module mandatory, Module optional ) {
		return optional == null || optional == NO_OP
			? mandatory
			: new MacroModule( mandatory, optional );
	}

	public static Module macro( Module... steps ) {
		return new MacroModule( steps );
	}

	private final Module[] steps;

	private MacroModule( Module... steps ) {
		super();
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
		return describe( "macro", steps );
	}

	private static final class ConstructorMacro
			implements Macro<Constructible<?>> {

		@Override
		public <T> Module expand( Binding<T> binding, Constructible<?> constructible ) {
			return binding.suppliedBy( CONSTRUCTOR,
					SuppliedBy.costructor( constructible.typed( binding.getType() ) ) );
		}

	}

	private static final class MethodMacro
			implements Macro<Producible<?>> {

		@Override
		public <T> Module expand( Binding<T> binding, Producible<?> producible ) {
			return binding.suppliedBy( METHOD,
					SuppliedBy.method( producible.typed( binding.getType() ) ) );
		}

	}

	private static final class SubstitutionMacro
			implements Macro<Instance<?>> {

		@Override
		public <T> Module expand( Binding<T> binding, Instance<?> with ) {
			final Type<? extends T> type = with.getType().castTo( binding.getType() );
			final Instance<T> bound = binding.getInstance();
			if ( !bound.getType().equalTo( type )
					|| !with.getName().isApplicableFor( bound.getName() ) ) {
				return macro(
						binding.suppliedBy( SUBSTITUTED, SuppliedBy.instance( with.typed( type ) ) ),
						implicitBindToConstructor( with, binding.source ) );
			}
			if ( type.getRawType().isInterface() ) {
				throw new IllegalArgumentException( "Interface type linked in a loop: " + bound
						+ " > " + with );
			}
			return new ExplicitConstructorMacroModule<T>( binding, type.getRawType() );
		}

	}

	private static final class ConfigurationMacro
			implements Macro<Configuring<?>> {

		@Override
		public <T> Module expand( Binding<T> binding, Configuring<?> by ) {
			return binding.suppliedBy( SUBSTITUTED,
					SuppliedBy.configuration( binding.getType(), by ) );
		}

	}

	private static final class ForwardMacro
			implements Macro<Class<?>> {

		@Override
		public <T> Module expand( Binding<T> binding, Class<?> to ) {
			return binding.suppliedBy( SUBSTITUTED,
					parametrizedInstance( anyOf( raw( to ).castTo( binding.getType() ) ) ) );
		}

	}

	private static final class ExpandMacro
			implements Macro<Object> {

		@Override
		public <T> Module expand( Binding<T> binding, Object value ) {
			if ( binding.supplier == null ) {
				throw new NullPointerException( "Binding has no supplier" );
			}
			return binding;
		}

	}

	static <T> Module implicitBindToConstructor( Instance<T> instance, Source source ) {
		Class<T> impl = instance.getType().getRawType();
		return metaclass( impl ).undeterminable()
			? null
			: new ImplicitConstructorMacroModule<T>( instance, source );
	}

	private static final class ExplicitConstructorMacroModule<T>
			extends BinderModule {

		private final Binding<T> binding;
		private final Class<? extends T> implementer;

		ExplicitConstructorMacroModule( Binding<T> binding, Class<? extends T> implementer ) {
			super( binding.source );
			this.binding = binding;
			this.implementer = implementer;
		}

		@Override
		protected void declare() {
			Bind bind = bind().per( binding.scope ).with( binding.getResource().getTarget() );
			on( bind ).bind( binding.getInstance() ).toConstructor( implementer );
		}

	}

	private static final class ImplicitConstructorMacroModule<T>
			extends BinderModule {

		private final Instance<T> instance;

		ImplicitConstructorMacroModule( Instance<T> instance, Source source ) {
			super( source );
			this.instance = instance;
		}

		@Override
		public void declare() {
			Constructor<T> constructor = bind().getInspector().constructorFor(
					instance.getType().getRawType() );
			if ( constructor != null ) {
				implicit().with( Target.ANY ).bind( instance ).to( constructor );
			}
		}

	}

}
