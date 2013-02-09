/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.Dependency.dependency;

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
import se.jbee.inject.Injector;
import se.jbee.inject.Injectron;
import se.jbee.inject.Packages;
import se.jbee.inject.Type;
import se.jbee.inject.util.Inject;
import se.jbee.inject.util.Suppliable;
import se.jbee.inject.util.TypeReflector;

/**
 * Utility to create an {@link Injector} context from {@link Bundle}s and {@link Module}s.
 * 
 * It allos to use {@link Edition}s and {@link Feature}s to modularize or customize context
 * configurations.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Bootstrap {

	public static Injector injector( Class<? extends Bundle> root ) {
		return injector( root, Edition.FULL );
	}

	public static Injector injector( Class<? extends Bundle> root, Edition edition ) {
		return injector( root, edition, Presets.NOTHING, Options.STANDARD );
	}

	public static Injector injector( Class<? extends Bundle> root, Edition edition,
			Presets presets, Options options ) {
		return injector( root, edition, presets, options, Inspect.DEFAULT );
	}

	public static Injector injector( Class<? extends Bundle> root, Edition edition,
			Presets presets, Options options, Inspector inspector ) {
		return injector( modulariser( edition, presets, options ).modularise( root ), inspector,
				Link.BUILDIN );
	}

	public static Injector injector( Module[] modules, Inspector inspector,
			Linker<Suppliable<?>> linker ) {
		return Inject.from( Suppliable.source( linker.link( inspector, modules ) ) );
	}

	public static Modulariser modulariser( Edition edition, Presets presets, Options options ) {
		return new BuildinBootstrapper( edition, presets, options );
	}

	public static Bundler bundler( Edition edition, Presets presets, Options options ) {
		return new BuildinBootstrapper( edition, presets, options );
	}

	public static Suppliable<?>[] suppliables( Class<? extends Bundle> root ) {
		return Link.BUILDIN.link( Inspect.DEFAULT, // 
				modulariser( Edition.FULL, Presets.NOTHING, Options.STANDARD ).modularise( root ) );
	}

	public static <T extends Enum<T> & Feature<T>> Edition edition( T... featured ) {
		return new FeatureEdition<T>( featured[0], EnumSet.of( featured[0], featured ) );
	}

	public static Edition edition( Packages included ) {
		return new PackagesEdition( included );
	}

	public static <T> Module module( PresetModule<T> module, Presets presets ) {
		return new LazyPresetModule<T>( module, presets );
	}

	public static void eagerSingletons( Injector injector ) {
		for ( Injectron<?> i : injector.resolve( dependency( Injectron[].class ) ) ) {
			if ( i.getExpiry().isNever() ) {
				instance( i );
			}
		}
	}

	public static <T> T instance( Injectron<T> injectron ) {
		return injectron.instanceFor( dependency( injectron.getResource().getInstance() ) );
	}

	public static void nonnullThrowsReentranceException( Object field ) {
		if ( field != null ) {
			throw new IllegalStateException( "Reentrance not allowed!" );
		}
	}

	private Bootstrap() {
		throw new UnsupportedOperationException( "util" );
	}

	private static class FeatureEdition<T extends Enum<T>>
			implements Edition {

		private final Feature<T> feature;
		private final EnumSet<T> featured;

		FeatureEdition( Feature<T> feature, EnumSet<T> featured ) {
			super();
			this.feature = feature;
			this.featured = featured;
		}

		@Override
		public boolean featured( Class<?> bundleOrModule ) {
			T f = feature.featureOf( bundleOrModule );
			return f == null || featured.contains( f );
		}
	}

	private static class PackagesEdition
			implements Edition {

		private final Packages included;

		PackagesEdition( Packages included ) {
			super();
			this.included = included;
		}

		@Override
		public boolean featured( Class<?> bundleOrModule ) {
			return included.contains( Type.raw( bundleOrModule ) );
		}

	}

	private static final class LazyPresetModule<T>
			implements Module {

		private final PresetModule<T> module;
		private final Presets presets;

		LazyPresetModule( PresetModule<T> module, Presets presets ) {
			super();
			this.module = module;
			this.presets = presets;
		}

		@Override
		public void declare( Bindings bindings, Inspector inspector ) {
			@SuppressWarnings ( "unchecked" )
			final T value = (T) presets.value( Type.supertype( PresetModule.class,
					Type.raw( module.getClass() ) ).parameter( 0 ) );
			module.declare( bindings, inspector, value );
		}
	}

	private static class BuildinBootstrapper
			implements Bootstrapper, Bundler, Modulariser {

		private final Map<Class<? extends Bundle>, Set<Class<? extends Bundle>>> bundleChildren = new IdentityHashMap<Class<? extends Bundle>, Set<Class<? extends Bundle>>>();
		private final Map<Class<? extends Bundle>, List<Module>> bundleModules = new IdentityHashMap<Class<? extends Bundle>, List<Module>>();
		private final Set<Class<? extends Bundle>> uninstalled = new HashSet<Class<? extends Bundle>>();
		private final Set<Class<? extends Bundle>> installed = new HashSet<Class<? extends Bundle>>();
		private final LinkedList<Class<? extends Bundle>> stack = new LinkedList<Class<? extends Bundle>>();
		private final Edition edition;
		private final Presets presets;
		final Options options;

		BuildinBootstrapper( Edition edition, Presets presets, Options options ) {
			super();
			this.edition = edition;
			this.presets = presets;
			this.options = options;
		}

		@Override
		public void install( Class<? extends Bundle> bundle ) {
			if ( uninstalled.contains( bundle ) || installed.contains( bundle ) ) {
				return;
			}
			if ( !edition.featured( bundle ) ) {
				uninstalled.add( bundle ); // this way we will never ask again - something not featured is finally not featured
				return;
			}
			installed.add( bundle );
			if ( !stack.isEmpty() ) {
				final Class<? extends Bundle> parent = stack.peek();
				Set<Class<? extends Bundle>> children = bundleChildren.get( parent );
				if ( children == null ) {
					children = new LinkedHashSet<Class<? extends Bundle>>();
					bundleChildren.put( parent, children );
				}
				children.add( bundle );
			}
			stack.push( bundle );
			TypeReflector.newInstance( bundle ).bootstrap( this );
			if ( stack.pop() != bundle ) {
				throw new IllegalStateException( bundle.getCanonicalName() );
			}
		}

		@Override
		public <C extends Enum<C>> void install( Class<? extends ModularBundle<C>> bundle,
				final Class<C> property ) {
			if ( !edition.featured( property ) ) {
				return;
			}
			TypeReflector.newInstance( bundle ).bootstrap( new ModularBootstrapper<C>() {

				@Override
				public void install( Class<? extends Bundle> bundle, C module ) {
					if ( options.isChosen( property, module ) ) { // null is a valid value to define what happens when no configuration is present
						BuildinBootstrapper.this.install( bundle );
					}
				}
			} );
		}

		@Override
		public <M extends Enum<M> & ModularBundle<M>> void install( M... modules ) {
			if ( modules.length > 0 ) {
				final M bundle = modules[0];
				if ( !edition.featured( bundle.getClass() ) ) {
					return;
				}
				final EnumSet<M> installing = EnumSet.of( bundle, modules );
				bundle.bootstrap( new ModularBootstrapper<M>() {

					@Override
					public void install( Class<? extends Bundle> bundle, M module ) {
						if ( installing.contains( module ) ) {
							BuildinBootstrapper.this.install( bundle );
						}
					}
				} );
			}
		}

		@Override
		public void install( Module module ) {
			Class<? extends Bundle> bundle = stack.peek();
			if ( uninstalled.contains( bundle ) || !edition.featured( module.getClass() ) ) {
				return;
			}
			List<Module> modules = bundleModules.get( bundle );
			if ( modules == null ) {
				modules = new ArrayList<Module>();
				bundleModules.put( bundle, modules );
			}
			modules.add( module );
		}

		@Override
		public <T> void install( PresetModule<T> module ) {
			install( module( module, presets ) );
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
			Set<Class<? extends Bundle>> installed = new LinkedHashSet<Class<? extends Bundle>>();
			addAllInstalledIn( root, installed );
			return Array.of( installed, Class.class );
		}

		private Module[] modulesOf( Class<? extends Bundle>[] bundles ) {
			List<Module> installed = new ArrayList<Module>( bundles.length );
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
		public <M extends Enum<M> & ModularBundle<M>> void uninstall( M... modules ) {
			if ( modules.length > 0 ) {
				final EnumSet<M> uninstalling = EnumSet.of( modules[0], modules );
				modules[0].bootstrap( new ModularBootstrapper<M>() {

					@Override
					public void install( Class<? extends Bundle> bundle, M module ) {
						if ( uninstalling.contains( module ) ) {
							uninstall( bundle );
						}
					}
				} );
			}
		}

		private void addAllInstalledIn( Class<? extends Bundle> bundle,
				Set<Class<? extends Bundle>> accu ) {
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
