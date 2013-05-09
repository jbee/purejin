package se.jbee.inject.bind;

import static se.jbee.inject.Type.returnType;
import static se.jbee.inject.bootstrap.BindingType.CONSTRUCTOR;
import static se.jbee.inject.bootstrap.BindingType.LINK;
import static se.jbee.inject.bootstrap.BindingType.METHOD;
import static se.jbee.inject.util.Metaclass.metaclass;
import static se.jbee.inject.util.ToString.describe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import se.jbee.inject.Instance;
import se.jbee.inject.Parameter;
import se.jbee.inject.Source;
import se.jbee.inject.Supplier;
import se.jbee.inject.Target;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Binding;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Macros;
import se.jbee.inject.bootstrap.Module;

public final class Macro
		implements Module {

	public static final Macros MACROS = new DefaultMacros();

	public static Module macro( Module mandatory, Module optional ) {
		return optional == null
			? mandatory
			: new Macro( mandatory, optional );
	}

	public static Module macro( Module... steps ) {
		return new Macro( steps );
	}

	private final Module[] steps;

	private Macro( Module... steps ) {
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

	private static class DefaultMacros
			implements Macros {

		DefaultMacros() {
			// make visible
		}

		@Override
		public <T> Module expand( Binding<T> binding ) {
			return binding;
		}

		@Override
		public <T> Module construct( Binding<T> binding, Constructor<? extends T> constructor,
				Parameter<?>... parameters ) {
			return binding.suppliedBy( CONSTRUCTOR, SuppliedBy.costructor( constructor, parameters ) );
		}

		@SuppressWarnings ( "unchecked" )
		@Override
		public <T> Module produce( Binding<T> binding, Object instance, Method method,
				Parameter<?>... parameters ) {
			Supplier<? extends T> supplier = (Supplier<? extends T>) SuppliedBy.method(
					returnType( method ), instance, method, parameters );
			return binding.suppliedBy( METHOD, supplier );
		}

		@Override
		public <T> Module link( Binding<T> binding, Instance<? extends T> instance ) {
			final Type<? extends T> type = instance.getType();
			final Instance<T> bound = binding.getInstance();
			if ( !bound.getType().equalTo( type )
					|| !instance.getName().isApplicableFor( bound.getName() ) ) {
				return macro( binding.suppliedBy( LINK, SuppliedBy.instance( instance ) ),
						implicitBindToConstructor( instance, binding.source ) );
			}
			if ( type.getRawType().isInterface() ) {
				throw new IllegalArgumentException( "Interface type linked in a loop: " + bound
						+ " > " + instance );
			}
			return new ConstructorMacro<T>( binding, type.getRawType() );
		}
	}

	static <T> Module implicitBindToConstructor( Instance<T> instance, Source source ) {
		Class<T> impl = instance.getType().getRawType();
		return metaclass( impl ).undeterminable()
			? null
			: new ImplicitConstructorMacro<T>( instance, source );
	}

	private static final class ConstructorMacro<T>
			extends BinderModule {

		private final Binding<T> binding;
		private final Class<? extends T> implementer;

		ConstructorMacro( Binding<T> binding, Class<? extends T> implementer ) {
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

	private static final class ImplicitConstructorMacro<T>
			extends BinderModule {

		private final Instance<T> instance;

		ImplicitConstructorMacro( Instance<T> instance, Source source ) {
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
