/*
 *  Copyright (c) 2012-2017, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Type.raw;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.jbee.inject.Array;
import se.jbee.inject.InconsistentBinding;
import se.jbee.inject.Injector;
import se.jbee.inject.Injectron;
import se.jbee.inject.Type;
import se.jbee.inject.config.Globals;
import se.jbee.inject.config.Options;
import se.jbee.inject.config.Presets;
import se.jbee.inject.container.Inject;

/**
 * Utility to create an {@link Injector} context from {@link Bundle}s and {@link Module}s.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Bootstrap {

	public static Injector injector( Class<? extends Bundle> root ) {
		return injector( root, Globals.STANDARD );
	}

	public static Injector injector( Class<? extends Bundle> root, Globals globals ) {
		return injector( root, Bindings.bindings( Macros.DEFAULT, Inspect.DEFAULT ), globals );
	}

	public static Injector injector( Class<? extends Bundle> root, Bindings bindings, Globals globals ) {
		return injector( bindings, modulariser( globals ).modularise( root ) );
	}

	public static Injector injector( Bindings bindings, Module[] modules ) {
		return Inject.container( Binding.disambiguate( bindings.declareFrom(modules)) );
	}

	public static Modulariser modulariser( Globals globals ) {
		return new BuildinBootstrapper( globals );
	}

	public static Bundler bundler( Globals globals ) {
		return new BuildinBootstrapper( globals );
	}

	public static Binding<?>[] bindings( Class<? extends Bundle> root, Bindings bindings, Globals globals ) {
		return Binding.disambiguate( bindings.declareFrom( modulariser( globals ).modularise( root ) ) );
	}

	public static <T> Module module( PresetModule<T> module, Presets presets ) {
		return new ModuleToPresetModule<>( module, presets );
	}

	public static void eagerSingletons( Injector injector ) {
		for ( Injectron<?> i : injector.resolve( dependency( Injectron[].class ) ) ) {
			if ( i.info().expiry.isNever() ) {
				instance( i ); // instantiate to make sure they exist in repository
			}
		}
	}

	public static <T> T instance( Injectron<T> injectron ) {
		return injectron.instanceFor( dependency( injectron.info().resource.instance ) );
	}

	public static void nonnullThrowsReentranceException( Object field ) {
		if ( field != null ) {
			throw new InconsistentBinding( "Reentrance not allowed!" );
		}
	}

	public static <T> T instance( Class<T> type ) {
		return Supply.constructor( Metaclass.accessible( Inspect.noArgsConstructor( type ) ) );
	}

	private Bootstrap() {
		throw new UnsupportedOperationException( "util" );
	}

	private static final class ModuleToPresetModule<T>
			implements Module {

		private final PresetModule<T> module;
		private final Presets presets;

		ModuleToPresetModule( PresetModule<T> module, Presets presets ) {
			super();
			this.module = module;
			this.presets = presets;
		}

		@Override
		public void declare( Bindings bindings ) {
			Type<?> valueType = Type.supertype( PresetModule.class, raw( module.getClass() ) ).parameter( 0 );
			@SuppressWarnings ( "unchecked" )
			final T value = (T) presets.value( valueType );
			module.declare( bindings, value );
		}
	}

	private static final class BuildinBootstrapper
			implements Bootstrapper, Bundler, Modulariser {

		private final Map<Class<? extends Bundle>, Set<Class<? extends Bundle>>> bundleChildren = new IdentityHashMap<>();
		private final Map<Class<? extends Bundle>, List<Module>> bundleModules = new IdentityHashMap<>();
		private final Set<Class<? extends Bundle>> uninstalled = new HashSet<>();
		private final Set<Class<? extends Bundle>> installed = new HashSet<>();
		private final LinkedList<Class<? extends Bundle>> stack = new LinkedList<>();
		private final Globals globals;

		BuildinBootstrapper( Globals globals ) {
			super();
			this.globals = globals;
		}

		@Override
		public void install( Class<? extends Bundle> bundle ) {
			if ( uninstalled.contains( bundle ) || installed.contains( bundle ) ) {
				return;
			}
			if ( !globals.edition.featured( bundle ) ) {
				uninstalled.add( bundle ); // this way we will never ask again - something not featured is finally not featured
				return;
			}
			installed.add( bundle );
			if ( !stack.isEmpty() ) {
				final Class<? extends Bundle> parent = stack.peek();
				Set<Class<? extends Bundle>> children = bundleChildren.get( parent );
				if ( children == null ) {
					children = new LinkedHashSet<>();
					bundleChildren.put( parent, children );
				}
				children.add( bundle );
			}
			stack.push( bundle );
			Bootstrap.instance( bundle ).bootstrap( this );
			if ( stack.pop() != bundle ) {
				throw new IllegalStateException( bundle.getCanonicalName() );
			}
		}

		@Override
		public <O extends Enum<O>> void install( Class<? extends OptionBundle<O>> bundle,
				final Class<O> property ) {
			if ( !globals.edition.featured( property ) ) {
				return;
			}
			final Options options = globals.options;
			Bootstrap.instance( bundle ).bootstrap( (bundleForOption, onOption) -> {
				if ( options.isChosen( property, onOption ) ) { // null is a valid value to define what happens when no configuration is present
					BuildinBootstrapper.this.install( bundleForOption );
				}
			});
		}

		@Override
		@SafeVarargs
		public final <O extends Enum<O> & OptionBundle<O>> void install( O... options ) {
			if ( options.length > 0 ) {
				final O option = options[0];
				if ( !globals.edition.featured( option.getClass() ) ) {
					return;
				}
				final EnumSet<O> installing = EnumSet.of( option, options );
				option.bootstrap( (bundle, onOption) -> {
					if ( installing.contains( onOption ) ) {
						BuildinBootstrapper.this.install( bundle );
					}
				});
			}
		}

		@Override
		public void install( Module module ) {
			Class<? extends Bundle> bundle = stack.peek();
			if ( uninstalled.contains( bundle ) || !globals.edition.featured( module.getClass() ) ) {
				return;
			}
			List<Module> modules = bundleModules.get( bundle );
			if ( modules == null ) {
				modules = new ArrayList<>();
				bundleModules.put( bundle, modules );
			}
			modules.add( module );
		}

		@Override
		public <T> void install( PresetModule<T> module ) {
			install( module( module, globals.presets ) );
		}

		@Override
		public Module[] modularise( Class<? extends Bundle> root ) {
			return modulesOf( bundle( root ) );
		}

		@SuppressWarnings ( "unchecked" )
		@Override
		public Class<? extends Bundle>[] bundle( Class<? extends Bundle> root ) {
			if ( !installed.contains( root ) ) {
				install( root );
			}
			Set<Class<? extends Bundle>> installed = new LinkedHashSet<>();
			addAllInstalledIn( root, installed );
			return Array.of( installed, Class.class );
		}

		private Module[] modulesOf( Class<? extends Bundle>[] bundles ) {
			List<Module> installed = new ArrayList<>( bundles.length );
			for ( Class<? extends Bundle> b : bundles ) {
				List<Module> modules = bundleModules.get( b );
				if ( modules != null ) {
					installed.addAll( modules );
				}
			}
			return Array.of( installed, Module.class );
		}

		@Override
		public void uninstall( Class<? extends Bundle> bundle ) {
			if ( uninstalled.contains( bundle ) ) {
				return;
			}
			uninstalled.add( bundle );
			installed.remove( bundle );
			for ( Set<Class<? extends Bundle>> c : bundleChildren.values() ) {
				c.remove( bundle );
			}
			bundleModules.remove( bundle ); // we are sure we don't need its modules
		}

		@Override
		@SafeVarargs
		public final <O extends Enum<O> & OptionBundle<O>> void uninstall( O... bundles ) {
			if ( bundles.length > 0 ) {
				final EnumSet<O> uninstalling = EnumSet.of( bundles[0], bundles );
				bundles[0].bootstrap( (bundle, onOption) -> {
					if ( uninstalling.contains( onOption ) ) {
						uninstall( bundle );
					}
				} );
			}
		}

		private void addAllInstalledIn( Class<? extends Bundle> bundle, Set<Class<? extends Bundle>> accu ) {
			accu.add( bundle );
			Set<Class<? extends Bundle>> children = bundleChildren.get( bundle );
			if ( children == null ) {
				return;
			}
			for ( Class<? extends Bundle> c : children ) {
				if ( !accu.contains( c ) ) {
					addAllInstalledIn( c, accu );
				}
			}
		}

	}
}
