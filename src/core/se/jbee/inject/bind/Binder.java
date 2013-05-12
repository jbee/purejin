/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.bind.Configuring.configuring;
import static se.jbee.inject.util.Constructible.constructible;
import static se.jbee.inject.util.Metaclass.metaclass;
import static se.jbee.inject.util.Producible.producible;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import se.jbee.inject.Array;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Packages;
import se.jbee.inject.Parameter;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Supplier;
import se.jbee.inject.Target;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.BindingType;
import se.jbee.inject.bootstrap.Inspector;
import se.jbee.inject.bootstrap.Module;
import se.jbee.inject.util.Factory;
import se.jbee.inject.util.Scoped;

/**
 * The default implementation of a fluent binder interface that provides a lot of utility methods to
 * improve readability and keep binding compact.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class Binder {

	public static RootBinder create( Bind bind ) {
		return new RootBinder( bind );
	}

	final RootBinder root;
	private final Bind bind;

	Binder( RootBinder root, Bind bind ) {
		super();
		this.root = root == null
			? (RootBinder) this
			: root;
		this.bind = bind;
	}

	Bind bind() {
		return bind;
	}

	public <E> TypedElementBinder<E> arraybind( Class<E[]> type ) {
		return new TypedElementBinder<E>( bind(), defaultInstanceOf( raw( type ) ) );
	}

	public <T> TypedBinder<T> autobind( Class<T> type ) {
		return autobind( Type.raw( type ) );
	}

	public <T> TypedBinder<T> autobind( Type<T> type ) {
		return on( bind().autobinding().asAuto() ).bind( type );
	}

	public <T> TypedBinder<T> bind( Class<T> type ) {
		return bind( Type.raw( type ) );
	}

	public <T> TypedBinder<T> bind( Instance<T> instance ) {
		return new TypedBinder<T>( bind(), instance );
	}

	public <T> TypedBinder<T> bind( Name name, Class<T> type ) {
		return bind( name, Type.raw( type ) );
	}

	public <T> TypedBinder<T> bind( Name name, Type<T> type ) {
		return bind( instance( name, type ) );
	}

	public <T> TypedBinder<T> bind( Type<T> type ) {
		return bind( defaultInstanceOf( type ) );
	}

	public void construct( Class<?> type ) {
		construct( ( defaultInstanceOf( raw( type ) ) ) );
	}

	public void construct( Instance<?> instance ) {
		bind( instance ).toConstructor();
	}

	public void construct( Name name, Class<?> type ) {
		construct( instance( name, raw( type ) ) );
	}

	public <T> TypedBinder<T> multibind( Class<T> type ) {
		return multibind( Type.raw( type ) );
	}

	public <T> TypedBinder<T> multibind( Instance<T> instance ) {
		return on( bind().asMulti() ).bind( instance );
	}

	public <T> TypedBinder<T> multibind( Name name, Class<T> type ) {
		return multibind( instance( name, Type.raw( type ) ) );
	}

	public <T> TypedBinder<T> multibind( Name name, Type<T> type ) {
		return multibind( instance( name, type ) );
	}

	public <T> TypedBinder<T> multibind( Type<T> type ) {
		return multibind( defaultInstanceOf( type ) );
	}

	public <T> TypedBinder<T> starbind( Class<T> type ) {
		return bind( Instance.anyOf( Type.raw( type ) ) );
	}

	public <T, C> ConfigBinder<T> configbind( Class<T> type ) {
		return configbind( raw( type ) );
	}

	public <T, C> ConfigBinder<T> configbind( Type<T> type ) {
		return new ConfigBinder<T>( root, type );
	}

	protected Binder on( Bind bind ) {
		return new Binder( root, bind );
	}

	protected final <I> void implicitBindToConstructor( Class<I> impl ) {
		implicitBindToConstructor( defaultInstanceOf( raw( impl ) ) );
	}

	protected final <I> void implicitBindToConstructor( Instance<I> instance ) {
		Class<I> impl = instance.getType().getRawType();
		if ( metaclass( impl ).undeterminable() ) {
			return;
		}
		Constructor<I> constructor = bind().getInspector().constructorFor( impl );
		if ( constructor != null ) {
			implicit().with( Target.ANY ).bind( instance ).to( constructor );
		}
	}

	protected final Binder implicit() {
		return on( bind().asImplicit() );
	}

	protected Binder with( Target target ) {
		return new Binder( root, bind().with( target ) );
	}

	public static class ConfigBinder<T> {

		private final RootBinder binder;
		private final Type<T> type;

		ConfigBinder( RootBinder binder, Type<T> type ) {
			super();
			this.binder = binder;
			this.type = type;
		}

		public <C> TypedBinder<T> on( Configuring<C> configuring, C value ) {
			binder.per( Scoped.INJECTION ).implicit().bind( type ).to( configuring );
			return binder.bind( configuring.name( value ), type );
		}

		public <C extends Enum<C>> TypedBinder<T> onOther( Class<C> valueType ) {
			return on( Name.DEFAULT, null, valueType, Configuring.ENUM );
		}

		public <C extends Enum<C>> TypedBinder<T> on( C value ) {
			return on( Name.DEFAULT, value, value.getDeclaringClass(), Configuring.ENUM );
		}

		public <C> TypedBinder<T> onOther( Name name, Class<C> valueType ) {
			return on( name, null, valueType, Configuring.TO_STRING );
		}

		public <C> TypedBinder<T> on( Name name, C value ) {
			return on( name, value, Configuring.TO_STRING );
		}

		public <C> TypedBinder<T> on( Name name, C value, Naming<? super C> naming ) {
			@SuppressWarnings ( "unchecked" )
			final Class<C> valueType = (Class<C>) value.getClass();
			return on( name, value, valueType, naming );
		}

		private <C> TypedBinder<T> on( Name name, C value, final Class<C> valueType,
				Naming<? super C> naming ) {
			return on( configuring( naming, instance( name, raw( valueType ) ) ), value );
		}

	}

	public static class InspectBinder {

		private final Inspector inspector;
		private final ScopedBinder binder;

		InspectBinder( Inspector inspector, RootBinder binder, Scope scope ) {
			super();
			this.inspector = inspector;
			this.binder = binder.on( binder.bind().asAuto() ).per( scope );
		}

		public void in( Class<?> implementor ) {
			in( implementor, new Parameter<?>[0] );
		}

		public void in( Object implementingInstance, Parameter<?>... parameters ) {
			bindMethodsIn( implementingInstance.getClass(), implementingInstance, parameters );
		}

		public void in( Class<?> implementer, Parameter<?>... parameters ) {
			boolean instanceMethods = bindMethodsIn( implementer, null, parameters );
			Constructor<?> c = inspector.constructorFor( implementer );
			if ( c == null ) {
				if ( instanceMethods ) {
					binder.root.per( Scoped.APPLICATION ).implicit().construct( implementer );
				}
			} else {
				if ( parameters.length == 0 ) {
					parameters = inspector.parametersFor( c );
				}
				bind( (Constructor<?>) c, parameters );
			}
		}

		private boolean bindMethodsIn( Class<?> implementer, Object instance,
				Parameter<?>[] parameters ) {
			boolean instanceMethods = false;
			for ( Method method : inspector.methodsIn( implementer ) ) {
				Type<?> returnType = Type.returnType( method );
				if ( !Type.VOID.equalTo( returnType ) ) {
					if ( parameters.length == 0 ) {
						parameters = inspector.parametersFor( method );
					}
					binder.bind( inspector.nameFor( method ), returnType ).to( instance, method,
							parameters );
					instanceMethods = instanceMethods || !Modifier.isStatic( method.getModifiers() );
				}
			}
			return instanceMethods;
		}

		private <T> void bind( Constructor<T> constructor, Parameter<?>... parameters ) {
			Name name = inspector.nameFor( constructor );
			Class<T> implementation = constructor.getDeclaringClass();
			if ( name.isDefault() ) {
				binder.autobind( implementation ).to( constructor, parameters );
			} else {
				binder.bind( name, implementation ).to( constructor, parameters );
				for ( Type<? super T> st : Type.raw( implementation ).supertypes() ) {
					if ( st.isInterface() ) {
						binder.implicit().bind( name, st ).to( name, implementation );
					}
				}
			}
		}

		public void in( Class<?> implementer, Class<?>... implementers ) {
			in( implementer );
			for ( Class<?> i : implementers ) {
				in( i );
			}
		}

		public void inModule() {
			in( binder.bind().source.getIdent() );
		}
	}

	public static class RootBinder
			extends ScopedBinder {

		RootBinder( Bind bind ) {
			super( null, bind );
		}

		public ScopedBinder per( Scope scope ) {
			return new ScopedBinder( root, bind().per( scope ) );
		}

		public RootBinder asDefault() {
			return on( bind().asDefault() );
		}

		//OPEN also allow naming for provided instances - this is used for value objects that become parameter

		public <T> void provide( Class<T> implementation, Parameter<?>... parameters ) {
			on( bind().autobinding().asProvided() ).bind( implementation ).toConstructor(
					parameters );
		}

		public <T> void require( Class<T> dependency ) {
			require( raw( dependency ) );
		}

		public <T> void require( Type<T> dependency ) {
			on( bind().asRequired() ).bind( dependency ).to( SuppliedBy.<T> required(),
					BindingType.REQUIRED );
		}

		@Override
		protected RootBinder on( Bind bind ) {
			return new RootBinder( bind );
		}

	}

	public static class ScopedBinder
			extends TargetedBinder {

		ScopedBinder( RootBinder root, Bind bind ) {
			super( root, bind );
		}

		public TargetedBinder injectingInto( Class<?> target ) {
			return injectingInto( raw( target ) );
		}

		public TargetedBinder injectingInto( Instance<?> target ) {
			return new TargetedBinder( root, bind().with( Target.targeting( target ) ) );
		}

		public TargetedBinder injectingInto( Name name, Class<?> type ) {
			return injectingInto( name, raw( type ) );
		}

		public TargetedBinder injectingInto( Name name, Type<?> type ) {
			return injectingInto( Instance.instance( name, type ) );
		}

		public TargetedBinder injectingInto( Type<?> target ) {
			return injectingInto( defaultInstanceOf( target ) );
		}

		public InspectBinder bind( Inspector inspector ) {
			return new InspectBinder( inspector, root, bind().scope );
		}

	}

	public static class TargetedBinder
			extends Binder {

		TargetedBinder( RootBinder root, Bind bind ) {
			super( root, bind );
		}

		public Binder in( Packages packages ) {
			return with( bind().target.in( packages ) );
		}

		public Binder inPackageAndSubPackagesOf( Class<?> type ) {
			return with( bind().target.inPackageAndSubPackagesOf( type ) );
		}

		public Binder inPackageOf( Class<?> type ) {
			return with( bind().target.inPackageOf( type ) );
		}

		public Binder inSubPackagesOf( Class<?> type ) {
			return with( bind().target.inSubPackagesOf( type ) );
		}

		public TargetedBinder within( Class<?> parent ) {
			return within( raw( parent ) );
		}

		public TargetedBinder within( Instance<?> parent ) {
			return new TargetedBinder( root, bind().within( parent ) );
		}

		public TargetedBinder within( Name name, Class<?> parent ) {
			return within( instance( name, raw( parent ) ) );
		}

		public TargetedBinder within( Name name, Type<?> parent ) {
			return within( instance( name, parent ) );
		}

		public TargetedBinder within( Type<?> parent ) {
			return within( anyOf( parent ) );
		}
	}

	public static class TypedBinder<T> {

		private final Bind bind;
		protected final Resource<T> resource;

		TypedBinder( Bind bind, Instance<T> instance ) {
			this( bind, new Resource<T>( instance, bind.target ) );
		}

		TypedBinder( Bind bind, Resource<T> resource ) {
			super();
			this.bind = bind;
			this.resource = resource;
		}

		public <I extends T> void to( Class<I> impl ) {
			to( Instance.anyOf( raw( impl ) ) );
		}

		public void to( Constructor<? extends T> constructor, Parameter<?>... parameters ) {
			expand( constructible( constructor, parameters ) );
		}

		void to( Object instance, Method method, Parameter<?>[] parameters ) {
			expand( producible( method, parameters, instance ) );
		}

		public void toMacro( Module macro ) {
			macro.declare( bind().bindings );
		}

		protected final void expand( Object value ) {
			toMacro( bind().bindings.getMacros().expand( bind().asMacro( resource ), value ) );
		}

		public void to( Factory<? extends T> factory ) {
			to( SuppliedBy.factory( factory ) );
		}

		public void to( Supplier<? extends T> supplier ) {
			to( supplier, BindingType.PREDEFINED );
		}

		public final void to( T constant ) {
			toConstant( constant );
		}

		public final void to( T constant1, T constant2 ) {
			onMulti().toConstant( constant1 ).toConstant( constant2 );
		}

		public final void to( T constant1, T constant2, T constant3 ) {
			onMulti().toConstant( constant1 ).toConstant( constant2 ).toConstant( constant3 );
		}

		public final void to( T constant1, T... constants ) {
			TypedBinder<T> multibinder = onMulti().toConstant( constant1 );
			for ( int i = 0; i < constants.length; i++ ) {
				multibinder.toConstant( constants[i] );
			}
		}

		public void toConstructor() {
			to( bind().getInspector().constructorFor( resource.getType().getRawType() ) );
		}

		public void toConstructor( Class<? extends T> impl, Parameter<?>... parameters ) {
			if ( metaclass( impl ).undeterminable() ) {
				throw new IllegalArgumentException( "Not a constructable type: " + impl );
			}
			to( bind().getInspector().constructorFor( impl ), parameters );
		}

		public void toConstructor( Parameter<?>... parameters ) {
			toConstructor( getType().getRawType(), parameters );
		}

		public <I extends T> void to( Name name, Class<I> type ) {
			to( instance( name, raw( type ) ) );
		}

		public <I extends T> void to( Name name, Type<I> type ) {
			to( instance( name, type ) );
		}

		public <I extends T> void to( Instance<I> instance ) {
			expand( instance );
		}

		public void to( Configuring<?> configuration ) {
			expand( configuration );
		}

		public <I extends T> void toParametrized( Class<I> impl ) {
			expand( impl );
		}

		public <I extends Supplier<? extends T>> void toSupplier( Class<I> impl ) {
			expand( impl ); //OPEN wrap so this case can be distinguished from toParametrized ?
		}

		protected final void to( Supplier<? extends T> supplier, BindingType type ) {
			expand( bind().asType( resource, type, supplier ) );
		}

		private TypedBinder<T> toConstant( T constant ) {
			to( SuppliedBy.constant( constant ) );
			return this;
		}

		final Bind bind() {
			return bind;
		}

		protected final Type<T> getType() {
			return resource.getType();
		}

		protected final TypedBinder<T> on( Bind bind ) {
			return new TypedBinder<T>( bind, resource );
		}

		protected final TypedBinder<T> onMulti() {
			return on( bind().asMulti() );
		}

	}

	/**
	 * This kind of bindings actually re-map the []-type so that the automatic behavior of returning
	 * all known instances of the element type will no longer be used whenever the bind made
	 * applies.
	 * 
	 * @author Jan Bernitt (jan@jbee.se)
	 * 
	 */
	public static class TypedElementBinder<E>
			extends TypedBinder<E[]> {

		TypedElementBinder( Bind bind, Instance<E[]> instance ) {
			super( bind.asMulti(), instance );
		}

		@SuppressWarnings ( "unchecked" )
		public void toElements( Parameter<? extends E> p1 ) {
			toElements( new Parameter[] { p1 } );
		}

		@SuppressWarnings ( "unchecked" )
		public void toElements( Parameter<? extends E> p1, Parameter<? extends E> p2 ) {
			toElements( new Parameter[] { p1, p2 } );
		}

		@SuppressWarnings ( "unchecked" )
		public void toElements( Parameter<? extends E> p1, Parameter<? extends E> p2,
				Parameter<? extends E> p3 ) {
			toElements( new Parameter[] { p1, p2, p3 } );
		}

		public void toElements( Parameter<? extends E>... parameters ) {
			expand( parameters );
		}

		public void toElements( E c1 ) {
			to( array( c1 ) );
		}

		public void toElements( E c1, E c2 ) {
			to( array( c1, c2 ) );
		}

		public void toElements( E c1, E c2, E c3 ) {
			to( array( c1, c2, c3 ) );
		}

		public void toElements( E... constants ) {
			to( array( constants ) );
		}

		@SuppressWarnings ( "unchecked" )
		private E[] array( Object... elements ) {
			Class<E[]> rawType = getType().getRawType();
			if ( elements.getClass() == rawType ) {
				return (E[]) elements;
			}
			E[] a = Array.newArrayInstance( rawType, elements.length );
			System.arraycopy( elements, 0, a, 0, a.length );
			return a;
		}
	}

}