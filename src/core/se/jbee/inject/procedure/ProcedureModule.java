/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.procedure;

import static java.util.Arrays.asList;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Dependency.pluginsFor;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.parameterTypes;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Type.returnType;
import static se.jbee.inject.container.Scoped.APPLICATION;
import static se.jbee.inject.container.Scoped.DEPENDENCY_TYPE;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.bootstrap.BoundParameter;
import se.jbee.inject.bootstrap.InjectionSite;
import se.jbee.inject.bootstrap.Inspect;
import se.jbee.inject.bootstrap.Inspector;
import se.jbee.inject.bootstrap.Invoke;
import se.jbee.inject.bootstrap.Metaclass;
import se.jbee.inject.bootstrap.Module;
import se.jbee.inject.container.Scoped;

/**
 * When binding {@link Procedure}s this {@link Module} can be extended.
 * 
 * It provides procedure-related bind methods.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class ProcedureModule
		extends BinderModule {

	/**
	 * The {@link Inspector} picks the {@link Method}s that are used to implement
	 * {@link Procedure}s. This abstraction allows to customize what methods are bound as
	 * {@link Procedure}s. The {@link Inspector#methodsIn(Class)} should return all methods in
	 * the given {@link Class} that should be used to implement a {@link Procedure}.
	 */
	public static final Instance<Inspector> PROCEDURE_INSPECTOR = instance( named(Procedure.class), raw( Inspector.class ) );

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <I,O> Dependency<Procedure<I,O>> procedureDependency(Type<I> input, Type<O> output) {
		Type type = raw(Procedure.class).parametized(input, output);
		return dependency(type);
	}	
	
	protected final void bindProceduresIn( Class<?> impl ) {
		plug(impl).into(Procedure.class);
	}
	
	protected final void discoverProceduresBy( Inspector inspector ) {
		bind( PROCEDURE_INSPECTOR ).to( inspector );
	}

	protected ProcedureModule() {
		super(Scoped.APPLICATION, ProcedureBaseModule.class);
	}

	private static final class ProcedureBaseModule
			extends BinderModule {

		@Override
		public void declare() {
			asDefault().per( DEPENDENCY_TYPE ).starbind( Procedure.class ).toSupplier( ProcedureSupplier.class );
			asDefault().per( APPLICATION ).bind( PROCEDURE_INSPECTOR ).to( Inspect.all().methods() );
			asDefault().per(APPLICATION).bind(Executor.class).to(DirectExecutor.class);
		}

	}
	
	static final class DirectExecutor implements Executor {

		@Override
		public <I, O> O run(Object impl, Method proc, Object[] args, Type<O> output, Type<I> input, I value) {
			return output.rawType.cast(Invoke.method(proc, impl, args));
		}
		
	}

	static final class ProcedureSupplier
			implements Supplier<Procedure<?, ?>> {

		/**
		 * A list of discovered methods for each implementation class.
		 */
		private final Map<Class<?>, Method[]> cachedMethods = new IdentityHashMap<Class<?>, Method[]>();
		/**
		 * All already created {@link Procedure}s identified by a unique function signature.
		 */
		private final Map<String, Procedure<?, ?>> cachedProcedures = new HashMap<String, Procedure<?, ?>>();

		private final Injector injector;
		private final Inspector inspect;
		private final Executor executor;
		private final Class<?>[] implementationClasses;

		public ProcedureSupplier( Injector injector ) {
			super();
			this.injector = injector;
			this.executor = injector.resolve(dependency(Executor.class));
			this.implementationClasses = injector.resolve( pluginsFor(Procedure.class) );
			this.inspect = injector.resolve( dependency( PROCEDURE_INSPECTOR ).injectingInto(ProcedureSupplier.class));
		}
		
		@Override
		public Procedure<?, ?> supply( Dependency<? super Procedure<?, ?>> dependency, Injector injector ) {
			Type<? super Procedure<?, ?>> type = dependency.type();
			return provide( type.parameter( 0 ), type.parameter( 1 ) );
		}

		@SuppressWarnings ( "unchecked" )
		private <I, O> Procedure<I, O> provide( Type<I> input, Type<O> output ) {
			final String key = input + "->" + output; // haskell like function signature
			Procedure<?, ?> proc = cachedProcedures.get( key );
			if ( proc == null ) {
				synchronized ( cachedProcedures ) {
					proc = cachedProcedures.get( key );
					if ( proc == null ) {
						Method method = resolveProcedure( input, output );
						Object impl = injector.resolve( dependency( method.getDeclaringClass() ) );
						proc = new ExecutorProcedure<I, O>(impl, method, input, output, executor, injector);
						cachedProcedures.put( key, proc );
					}
				}
			}
			return (Procedure<I, O>) proc;
		}

		private <I, O> Method resolveProcedure( Type<I> input, Type<O> output ) {
			for ( Class<?> impl : implementationClasses ) {
				for ( Method proc : procedures( impl ) ) {
					Type<?> rt = returnType( proc );
					if ( rt.equalTo( output ) ) {
						if ( input.equalTo( Type.VOID ) ) {
							if ( proc.getParameterTypes().length == 0 ) {
								return proc;
							}
						} else {
							for ( Type<?> pt : parameterTypes( proc ) ) {
								if ( pt.equalTo( input ) ) {
									return proc;
								}
							}
						}
					}
				}
			}
			throw new UnresolvableDependency.NoMethodForDependency( output, input );
		}

		private Method[] procedures( Class<?> impl ) {
			Method[] methods = cachedMethods.get( impl );
			if ( methods != null ) {
				return methods;
			}
			synchronized ( cachedMethods ) {
				methods = cachedMethods.get( impl );
				if ( methods == null ) {
					methods = inspect.methodsIn( impl );
					cachedMethods.put( impl, methods );
				}
			}
			return methods;
		}
	}
	
	private static final class ExecutorProcedure<I,O> implements Procedure<I, O> {
		
		private final Object impl;
		private final Method proc;
		private final Type<I> input;
		private final Type<O> output;
		
		private final Executor executor;
		private final Injector injector;

		private final InjectionSite injection;
		private final int inputIndex;
		
		public ExecutorProcedure(Object impl, Method proc, Type<I> input, Type<O> output, Executor executor, Injector injector) {
			super();
			this.impl = impl;
			this.proc = Metaclass.accessible(proc);
			this.input = input;
			this.output = output;
			this.executor = executor;
			this.injector = injector;
			Type<?>[] types = Type.parameterTypes(proc);
			this.injection = new InjectionSite(dependency(output).injectingInto(proc.getDeclaringClass()), injector, BoundParameter.bind(types, BoundParameter.constant(input, null)));
			this.inputIndex = asList(types).indexOf(input);
		}
		
		@Override
		public O run(I input) throws ProcedureMalfunction {
			try {
				Object[] args = injection.args(injector);
				if (inputIndex >= 0) {
					args[inputIndex] = input;
				}
				return executor.run(impl, proc, args, output, this.input, input);
			} catch (UnresolvableDependency e) {
				throw new ProcedureMalfunction("Failed to provide all indirect arguments!", e);
			}
		}
	}

}