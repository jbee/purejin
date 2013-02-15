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
import static se.jbee.inject.bind.SuppliedBy.constant;
import static se.jbee.inject.bind.SuppliedBy.parametrizedInstance;
import static se.jbee.inject.bind.SuppliedBy.provider;
import static se.jbee.inject.util.Metaclass.metaclass;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import se.jbee.inject.DeclarationType;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Packages;
import se.jbee.inject.Parameter;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Source;
import se.jbee.inject.Supplier;
import se.jbee.inject.Target;
import se.jbee.inject.Type;
import se.jbee.inject.util.Factory;
import se.jbee.inject.util.Provider;

/**
 * The default implementation of a fluent binder interface that provides a lot of utility methods to
 * improve readability and keep binding compact.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class Binder {

	public static Bindings autobinding( Bindings delegate ) {
		return new AutobindBindings( delegate );
	}

	public static RootBinder create( Bindings bindings, Inspector inspector, Source source,
			Scope scope ) {
		return new RootBinder( bindings, inspector, source, scope );
	}

	final Bindings bindings;
	final Inspector inspector;
	final Source source;
	final Scope scope;
	final Target target;

	Binder( Bindings bindings, Inspector inspector, Source source, Scope scope, Target target ) {
		super();
		this.bindings = bindings;
		this.inspector = inspector;
		this.source = source;
		this.scope = scope;
		this.target = target;
	}

	public <E> TypedElementBinder<E> arraybind( Class<E[]> type ) {
		return new TypedElementBinder<E>( this, defaultInstanceOf( raw( type ) ) );
	}

	public <T> TypedBinder<T> autobind( Class<T> type ) {
		return autobind( Type.raw( type ) );
	}

	public <T> TypedBinder<T> autobind( Type<T> type ) {
		return into( autobinding( bindings ) ).auto().bind( type );
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
		return multi().bind( instance );
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

	protected final <T> void bind( Resource<T> resource, Supplier<? extends T> supplier ) {
		bindings.add( resource, supplier, scope, source );
	}

	protected final Binder implicit() {
		return with( source.typed( DeclarationType.IMPLICIT ) );
	}

	protected final <I> void implicitBindToConstructor( Class<I> impl ) {
		implicitBindToConstructor( defaultInstanceOf( raw( impl ) ) );
	}

	protected final <I> void implicitBindToConstructor( Instance<I> instance ) {
		Class<I> impl = instance.getType().getRawType();
		if ( metaclass( impl ).undeterminable() ) {
			return;
		}
		Constructor<I> constructor = inspector.constructorFor( impl );
		if ( constructor != null ) {
			implicit().with( Target.ANY ).bind( instance ).to( constructor );
		}
	}

	protected Binder into( Bindings bindings ) {
		return new Binder( bindings, inspector, source, scope, target );
	}

	protected Binder multi() {
		return with( source.typed( DeclarationType.MULTI ) );
	}

	protected Binder auto() {
		return with( source.typed( DeclarationType.AUTO ) );
	}

	protected Binder with( Source source ) {
		return new Binder( bindings, inspector, source, scope, target );
	}

	protected Binder with( Target target ) {
		return new Binder( bindings, inspector, source, scope, target );
	}

	public static class InspectBinder {

		private final Inspector inspector;
		private final RootBinder binder;

		InspectBinder( Inspector inspector, RootBinder binder ) {
			super();
			this.inspector = inspector;
			this.binder = binder.with( binder.source.typed( DeclarationType.AUTO ) );
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
					binder.implicit().bind( implementor ).toConstructor();
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
			in( binder.source.getIdent() );
		}
	}

	public static class RootBinder
			extends ScopedBinder {

		RootBinder( Bindings bindings, Inspector inspector, Source source, Scope scope ) {
			super( bindings, inspector, source, scope );
		}

		public ScopedBinder per( Scope scope ) {
			return new ScopedBinder( bindings, inspector, source, scope );
		}

		public InspectBinder bind( Inspector inspector ) {
			return new InspectBinder( inspector, this );
		}

		public RootBinder asDefault() {
			return with( source.typed( DeclarationType.DEFAULT ) );
		}

		@Override
		protected RootBinder into( Bindings bindings ) {
			return new RootBinder( bindings, inspector, source, scope );
		}

		protected RootBinder using( Inspector inspector ) {
			return new RootBinder( bindings, inspector, source, scope );
		}

		@Override
		protected RootBinder with( Source source ) {
			return new RootBinder( bindings, inspector, source, scope );
		}
	}

	public static class ScopedBinder
			extends TargetedBinder {

		ScopedBinder( Bindings bindings, Inspector inspector, Source source, Scope scope ) {
			super( bindings, inspector, source, scope, Target.ANY );
		}

		public TargetedBinder injectingInto( Class<?> target ) {
			return injectingInto( raw( target ) );
		}

		public TargetedBinder injectingInto( Instance<?> target ) {
			return new TargetedBinder( bindings, inspector, source, scope,
					Target.targeting( target ) );
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

	}

	public static class TargetedBinder
			extends Binder {

		TargetedBinder( Bindings bindings, Inspector inspector, Source source, Scope scope,
				Target target ) {
			super( bindings, inspector, source, scope, target );
		}

		public Binder in( Packages packages ) {
			return with( target.in( packages ) );
		}

		public Binder inPackageAndSubPackagesOf( Class<?> type ) {
			return with( target.inPackageAndSubPackagesOf( type ) );
		}

		public Binder inPackageOf( Class<?> type ) {
			return with( target.inPackageOf( type ) );
		}

		public Binder inSubPackagesOf( Class<?> type ) {
			return with( target.inSubPackagesOf( type ) );
		}

		public TargetedBinder within( Class<?> parent ) {
			return within( raw( parent ) );
		}

		public TargetedBinder within( Instance<?> parent ) {
			return new TargetedBinder( bindings, inspector, source, scope, target.within( parent ) );
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
			this( binder, new Resource<T>( instance, binder.target ) );
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

		public void to( Provider<? extends T> provider ) {
			to( provider( provider ) );
			implicitBindToConstant( provider );
		}

		public void to( Supplier<? extends T> supplier ) {
			binder.bind( resource, supplier );
		}

		public void to( T constant ) {
			toConstant( constant );
		}

		public void to( T constant1, T constant2 ) {
			multi().toConstant( constant1 ).toConstant( constant2 );
		}

		public final void to( T constant1, T... constants ) {
			TypedBinder<T> multibinder = multi().toConstant( constant1 );
			for ( int i = 0; i < constants.length; i++ ) {
				multibinder.toConstant( constants[i] );
			}
		}

		public void to( T constant1, T constant2, T constant3 ) {
			multi().toConstant( constant1 ).toConstant( constant2 ).toConstant( constant3 );
		}

		public void toConstructor() {
			to( binder.inspector.constructorFor( resource.getType().getRawType() ) );
		}

		public void toConstructor( Class<? extends T> impl, Parameter<?>... parameters ) {
			if ( metaclass( impl ).undeterminable() ) {
				throw new IllegalArgumentException( "Not a constructable type: " + impl );
			}
			to( SuppliedBy.costructor( binder.inspector.constructorFor( impl ), parameters ) );
		}

		public void toConstructor( Parameter<?>... parameters ) {
			toConstructor( resource.getType().getRawType(), parameters );
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

		protected final TypedBinder<T> multi() {
			return new TypedBinder<T>( binder.multi(), resource );
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
			return SuppliedBy.costructor( binder.inspector.constructorFor( instance.getType().getRawType() ) );
		}

		@SuppressWarnings ( "unchecked" )
		private <P> void implicitBindToConstant( Provider<P> provider ) {
			Type<Provider<P>> providerType = (Type<Provider<P>>) Type.supertype( Provider.class,
					raw( provider.getClass() ) );
			binder.implicit().bind( defaultInstanceOf( providerType ) ).to(
					SuppliedBy.constant( provider ) );
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
			super( binder.multi(), instance );
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
			to( SuppliedBy.elements( getType().getRawType(), elements ) );
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

	private static class AutobindBindings
			implements Bindings {

		private final Bindings delegate;

		AutobindBindings( Bindings delegate ) {
			super();
			this.delegate = delegate;
		}

		@Override
		public <T> void add( Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
				Source source ) {
			delegate.add( resource, supplier, scope, source );
			Type<T> type = resource.getType();
			for ( Type<? super T> supertype : type.supertypes() ) {
				// Object is of cause a superclass of everything but not indented when doing auto-binds
				if ( supertype.getRawType() != Object.class ) {
					delegate.add( resource.typed( supertype ), supplier, scope, source );
				}
			}
		}
	}
}