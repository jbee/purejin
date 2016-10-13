/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.bootstrap.Metaclass.metaclass;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import se.jbee.inject.Array;
import se.jbee.inject.InconsistentBinding;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Packages;
import se.jbee.inject.Parameter;
import se.jbee.inject.Resource;
import se.jbee.inject.Source;
import se.jbee.inject.Supplier;
import se.jbee.inject.Target;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Binding;
import se.jbee.inject.bootstrap.BindingType;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.BoundConstructor;
import se.jbee.inject.bootstrap.BoundMethod;
import se.jbee.inject.bootstrap.Inspector;
import se.jbee.inject.bootstrap.Supply;
import se.jbee.inject.container.Factory;
import se.jbee.inject.container.Scope;
import se.jbee.inject.container.Scoped;

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
		this.bind = bind.source == null ? bind.with(Source.source(getClass())) : bind;
	}

	Bind bind() {
		return bind; // !ATTENTION! This method might be overridden to update Bind properties - do not access field directly
	}

	public <E> TypedElementBinder<E> arraybind( Class<E[]> type ) {
		return new TypedElementBinder<>( bind(), defaultInstanceOf( raw( type ) ) );
	}

	public <T> TypedBinder<T> autobind( Class<T> type ) {
		return autobind( Type.raw( type ) );
	}

	public <T> TypedBinder<T> autobind( Type<T> type ) {
		return on( bind().asAuto() ).bind( type );
	}

	public <T> TypedBinder<T> bind( Class<T> type ) {
		return bind( Type.raw( type ) );
	}

	public <T> TypedBinder<T> bind( Instance<T> instance ) {
		return new TypedBinder<>( bind(), instance );
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
		return multibind( raw( type ) );
	}

	public <T> TypedBinder<T> multibind( Instance<T> instance ) {
		return on( bind().asMulti() ).bind( instance );
	}

	public <T> TypedBinder<T> multibind( Name name, Class<T> type ) {
		return multibind( instance( name, raw( type ) ) );
	}

	public <T> TypedBinder<T> multibind( Name name, Type<T> type ) {
		return multibind( instance( name, type ) );
	}

	public <T> TypedBinder<T> multibind( Type<T> type ) {
		return multibind( defaultInstanceOf( type ) );
	}

	public <T> TypedBinder<T> starbind( Class<T> type ) {
		return bind( anyOf( raw( type ) ) );
	}
	
	public <T> PluginBinder<T> plug( Class<T> plugin ) {
		return new PluginBinder<>( on(bind()), plugin);
	}
	
	protected Binder on( Bind bind ) {
		return new Binder( root, bind );
	}

	protected final Binder implicit() {
		return on( bind().asImplicit() );
	}

	protected Binder with( Target target ) {
		return new Binder( root, bind().with( target ) );
	}

	public static class PluginBinder<T> {
		
		private final Binder binder;
		private final Class<T> plugin;

		PluginBinder(Binder binder, Class<T> plugin) {
			this.binder = binder;
			this.plugin = plugin;
		}

		public void into( Class<?> pluginPoint ) {
			binder.multibind(Name.named(pluginPoint.getCanonicalName()+":"+plugin.getCanonicalName()), Class.class).to(plugin);
			binder.implicit().construct(plugin);
			// we allow both collections of classes that have a common super-type or collections that don't
			if (raw(plugin).isAssignableTo(raw(pluginPoint).asUpperBound())) {
				// if they have a common super-type the plugin is bound as an implementation
				@SuppressWarnings("unchecked")
				Class<? super T> pp = (Class<? super T>) pluginPoint;
				binder.multibind(pp).to(plugin);
			}
		}
	}
	
	public static class InspectBinder {

		private final Inspector inspector;
		private final ScopedBinder binder;

		InspectBinder( Inspector inspector, RootBinder binder, Scope scope ) {
			super();
			this.inspector = inspector;
			this.binder = binder.on( binder.bind().asAuto() ).on( binder.bind().next() ).per( scope );
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
			// isn#t this a bit like provide?
			Name name = inspector.nameFor( constructor );
			Class<T> impl = constructor.getDeclaringClass();
			if ( name.isDefault() ) {
				binder.autobind( impl ).to( constructor, parameters );
			} else {
				binder.bind( name, impl ).to( constructor, parameters );
				for ( Type<? super T> st : Type.raw( impl ).supertypes() ) {
					if ( st.isInterface() ) {
						binder.implicit().bind( name, st ).to( name, impl );
					}
				}
			}
		}

		public void in( Class<?> impl, Class<?>... more ) {
			in( impl );
			for ( Class<?> i : more ) {
				in( i );
			}
		}

		public void inModule() {
			in( binder.bind().source.ident );
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
			on( bind().asProvided() ).bind( implementation ).toConstructor( parameters );
		}

		public <T> void require( Class<T> dependency ) {
			require( raw( dependency ) );
		}

		public <T> void require( Type<T> dependency ) {
			on( bind().asRequired() )
				.bind( dependency ).to( Supply.<T> required(), BindingType.REQUIRED );
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
			this( bind.next(), new Resource<>( instance, bind.target ) );
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
			expand( BoundConstructor.bind( constructor, parameters ) );
		}

		protected final void to( Object instance, Method method, Parameter<?>[] parameters ) {
			expand( BoundMethod.bind( instance, method, Type.returnType( method ), parameters ) );
		}
		
		protected final void expand( Object value ) {
			declareBindingsIn( bind().asType( resource, BindingType.MACRO, null ), value  );
		}
		
		protected final void expand( BindingType type, Supplier<? extends T> supplier ) {
			Binding<T> binding = bind().asType( resource, type, supplier );
			declareBindingsIn( binding, binding );
		}

		private void declareBindingsIn( Binding<?> binding, Object value ) {
			Bindings bindings = bind().bindings;
			bindings.macros.expandInto(bindings, binding, value);
		}

		public void to( Factory<? extends T> factory ) {
			to( Supply.factory( factory ) );
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

		@SafeVarargs
		public final void to( T constant1, T... constants ) {
			TypedBinder<T> multibinder = onMulti().toConstant( constant1 );
			for ( int i = 0; i < constants.length; i++ ) {
				multibinder.toConstant( constants[i] );
			}
		}

		public void toConstructor() {
			to( bind().inspector().constructorFor( resource.type().rawType ) );
		}

		public void toConstructor( Class<? extends T> impl, Parameter<?>... parameters ) {
			if ( metaclass( impl ).undeterminable() ) {
				throw new InconsistentBinding( "Not a constructable type: " + impl );
			}
			to( bind().inspector().constructorFor( impl ), parameters );
		}

		public void toConstructor( Parameter<?>... parameters ) {
			toConstructor( getType().rawType, parameters );
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

		public <I extends T> void toParametrized( Class<I> impl ) {
			expand( impl );
		}

		public <I extends Supplier<? extends T>> void toSupplier( Class<I> impl ) {
			expand( defaultInstanceOf( raw( impl ) ) );
		}

		protected final void to( Supplier<? extends T> supplier, BindingType type ) {
			expand( type, supplier);
		}

		private TypedBinder<T> toConstant( T constant ) {
			to( Supply.constant( constant ) );
			return this;
		}

		final Bind bind() {
			return bind;
		}

		protected final Type<T> getType() {
			return resource.type();
		}

		protected final TypedBinder<T> on( Bind bind ) {
			return new TypedBinder<>( bind, resource );
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
			super( bind.asMulti().next(), instance );
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

		@SafeVarargs
		public final void toElements( Parameter<? extends E>... parameters ) {
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

		@SafeVarargs
		public final void toElements( E... constants ) {
			to( array( constants ) );
		}

		@SuppressWarnings ( "unchecked" )
		private E[] array( Object... elements ) {
			Class<E[]> rawType = getType().rawType;
			if ( elements.getClass() == rawType ) {
				return (E[]) elements;
			}
			Object[] a = Array.newInstance( getType().baseType().rawType, elements.length );
			System.arraycopy( elements, 0, a, 0, a.length );
			return (E[]) a;
		}
	}

}