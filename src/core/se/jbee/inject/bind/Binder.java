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
import static se.jbee.inject.bind.SuppliedBy.constant;
import static se.jbee.inject.bind.SuppliedBy.parametrizedInstance;
import static se.jbee.inject.util.Metaclass.metaclass;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Packages;
import se.jbee.inject.Parameter;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Supplier;
import se.jbee.inject.Target;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Inspector;
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
		return new TypedElementBinder<E>( this, defaultInstanceOf( raw( type ) ) );
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
		return new TypedBinder<T>( this, instance );
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

	protected final <T> void bind( Resource<T> resource, Supplier<? extends T> supplier ) {
		Bind b = bind();
		b.bindings.add( resource, supplier, b.scope, b.source );
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
		Constructor<I> constructor = bind().inspector.constructorFor( impl );
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

		public void in( Class<?> implementor, Parameter<?>... parameters ) {
			boolean instanceMethods = bindMethodsIn( implementor, null, parameters );
			Constructor<?> c = inspector.constructorFor( implementor );
			if ( c == null ) {
				if ( instanceMethods ) {
					binder.root.per( Scoped.APPLICATION ).implicit().construct( implementor );
				}
			} else {
				if ( parameters.length == 0 ) {
					parameters = inspector.parametersFor( c );
				}
				bind( (Constructor<?>) c, parameters );
			}
		}

		private boolean bindMethodsIn( Class<?> implementor, Object instance,
				Parameter<?>[] parameters ) {
			boolean instanceMethods = false;
			for ( Method method : inspector.methodsIn( implementor ) ) {
				Type<?> returnType = Type.returnType( method );
				if ( !Type.VOID.equalTo( returnType ) ) {
					if ( parameters.length == 0 ) {
						parameters = inspector.parametersFor( method );
					}
					bind( returnType, method, instance, parameters );
					instanceMethods = instanceMethods || !Modifier.isStatic( method.getModifiers() );
				}
			}
			return instanceMethods;
		}

		private <T> void bind( Type<T> returnType, Method method, Object instance,
				Parameter<?>[] parameters ) {
			binder.bind( inspector.nameFor( method ), returnType ).to(
					SuppliedBy.method( returnType, method, instance, parameters ) );
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

		public void in( Class<?> implementor, Class<?>... implementors ) {
			in( implementor );
			for ( Class<?> i : implementors ) {
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

		//TODO also allow naming for provided instances - this is used for value objects that become parameter

		public <T> void provide( Class<T> implementation, Parameter<?>... parameters ) {
			on( bind().autobinding().asProvided() ).bind( implementation ).toConstructor(
					parameters );
		}

		public <T> void require( Class<T> dependency ) {
			require( raw( dependency ) );
		}

		public <T> void require( Type<T> dependency ) {
			on( bind().asRequired() ).bind( dependency ).to( SuppliedBy.<T> required() );
		}

		@Override
		protected RootBinder on( Bind bind ) {
			return new RootBinder( bind );
		}

	}

	public static class ScopedBinder
			extends TargetedBinder {

		ScopedBinder( RootBinder root, Bind bind ) {
			super( root, bind ); // bindings, inspector, source, scope, Target.ANY );
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

		/**
		 * The binder instance who's {@link RichBasicBinder#assemble(Instance)} method had been
		 * called to get to this {@link TypedBinder}.
		 */
		private final Binder binder;
		private final Resource<T> resource;

		TypedBinder( Binder binder, Instance<T> instance ) {
			this( binder, new Resource<T>( instance, binder.bind().target ) );
		}

		TypedBinder( Binder binder, Resource<T> resource ) {
			super();
			this.binder = binder;
			this.resource = resource;
		}

		public <I extends T> void to( Class<I> impl ) {
			to( Instance.anyOf( raw( impl ) ) );
		}

		public void to( Constructor<? extends T> constructor, Parameter<?>... parameters ) {
			to( SuppliedBy.costructor( constructor, parameters ) );
		}

		public void to( Factory<? extends T> factory ) {
			to( SuppliedBy.factory( factory ) );
		}

		public <I extends T> void to( Instance<I> instance ) {
			to( supply( instance ) );
		}

		public <I extends T> void to( Name name, Class<I> type ) {
			to( instance( name, raw( type ) ) );
		}

		public <I extends T> void to( Name name, Type<I> type ) {
			to( instance( name, type ) );
		}

		public void to( Supplier<? extends T> supplier ) {
			binder.bind( resource, supplier );
		}

		public void to( T constant ) {
			toConstant( constant );
		}

		public void to( T constant1, T constant2 ) {
			on( binder.bind().asMulti() ).toConstant( constant1 ).toConstant( constant2 );
		}

		public final void to( T constant1, T... constants ) {
			TypedBinder<T> multibinder = on( binder.bind().asMulti() ).toConstant( constant1 );
			for ( int i = 0; i < constants.length; i++ ) {
				multibinder.toConstant( constants[i] );
			}
		}

		public void to( T constant1, T constant2, T constant3 ) {
			on( binder.bind().asMulti() ).toConstant( constant1 ).toConstant( constant2 ).toConstant(
					constant3 );
		}

		public void toConstructor() {
			to( binder.bind().inspector.constructorFor( resource.getType().getRawType() ) );
		}

		public void toConstructor( Class<? extends T> impl, Parameter<?>... parameters ) {
			if ( metaclass( impl ).undeterminable() ) {
				throw new IllegalArgumentException( "Not a constructable type: " + impl );
			}
			to( binder.bind().inspector.constructorFor( impl ), parameters );
		}

		public void toConstructor( Parameter<?>... parameters ) {
			toConstructor( getType().getRawType(), parameters );
		}

		public void to( Configuring<?> configuration ) {
			to( SuppliedBy.configuration( getType(), configuration ) );
		}

		public <I extends T> void toParametrized( Class<I> impl ) {
			to( parametrizedInstance( Instance.anyOf( raw( impl ) ) ) );
		}

		public <I extends Supplier<? extends T>> void toSupplier( Class<I> impl ) {
			to( SuppliedBy.reference( impl ) );
			implicitBindToConstructor( defaultInstanceOf( raw( impl ) ) );
		}

		protected final Type<T> getType() {
			return resource.getType();
		}

		protected final TypedBinder<T> on( Bind bind ) {
			return new TypedBinder<T>( binder.on( bind ), resource );
		}

		<I> Supplier<I> supply( Class<I> impl ) {
			return supply( Instance.anyOf( raw( impl ) ) );
		}

		<I> Supplier<I> supply( Instance<I> instance ) {
			if ( !resource.getInstance().equalTo( instance ) ) {
				implicitBindToConstructor( instance );
				return SuppliedBy.instance( instance );
			}
			if ( instance.getType().getRawType().isInterface() ) {
				throw new IllegalArgumentException( "Interface type linked in a loop: "
						+ resource.getInstance() + " > " + instance );
			}
			return SuppliedBy.costructor( binder.bind().inspector.constructorFor( instance.getType().getRawType() ) );
		}

		private <I> void implicitBindToConstructor( Instance<I> instance ) {
			binder.implicitBindToConstructor( instance );
		}

		private TypedBinder<T> toConstant( T constant ) {
			to( SuppliedBy.constant( constant ) );
			return this;
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

		TypedElementBinder( Binder binder, Instance<E[]> instance ) {
			super( binder.on( binder.bind().asMulti() ), instance );
		}

		@SuppressWarnings ( "unchecked" )
		public void to( Supplier<? extends E> supplier1, Supplier<? extends E> supplier2 ) {
			to( (Supplier<? extends E>[]) new Supplier<?>[] { supplier1, supplier2 } );
		}

		@SuppressWarnings ( "unchecked" )
		public void to( Supplier<? extends E> supplier1, Supplier<? extends E> supplier2,
				Supplier<? extends E> supplier3 ) {
			to( (Supplier<? extends E>[]) new Supplier<?>[] { supplier1, supplier2, supplier3 } );
		}

		public void to( Supplier<? extends E>[] elements ) {
			to( SuppliedBy.references( getType().getRawType(), elements ) );
		}

		@SuppressWarnings ( "unchecked" )
		public void toElements( Class<? extends E>... impls ) {
			Supplier<? extends E>[] suppliers = (Supplier<? extends E>[]) new Supplier<?>[impls.length];
			for ( int i = 0; i < impls.length; i++ ) {
				suppliers[i] = supply( impls[i] );
			}
			to( suppliers );
		}

		public void toElements( Class<? extends E> impl1, Class<? extends E> impl2 ) {
			to( supply( impl1 ), supply( impl2 ) );
		}

		public void toElements( Class<? extends E> impl1, Class<? extends E> impl2,
				Class<? extends E> impl3 ) {
			to( supply( impl1 ), supply( impl2 ), supply( impl3 ) );
		}

		@SuppressWarnings ( "unchecked" )
		public void toElements( Class<? extends E> impl1, Class<? extends E> impl2,
				Class<? extends E> impl3, Class<? extends E> impl4 ) {
			to( (Supplier<? extends E>[]) new Supplier<?>[] { supply( impl1 ), supply( impl2 ),
					supply( impl3 ), supply( impl4 ) } );
		}

		public void toElements( E constant1, E constant2 ) {
			to( constant( constant1 ), constant( constant2 ) );
		}

	}

}