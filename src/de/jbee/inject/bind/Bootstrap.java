package de.jbee.inject.bind;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.jbee.inject.Injector;
import de.jbee.inject.Suppliable;
import de.jbee.inject.SuppliableInjector;
import de.jbee.inject.util.TypeReflector;

public final class Bootstrap {

	public static Injector injector( Class<? extends Bundle> root ) {
		return injector( root, Edition.FULL );
	}

	public static Injector injector( Class<? extends Bundle> root, Edition edition ) {
		return injector( root, edition, Constants.NONE );
	}

	public static Injector injector( Class<? extends Bundle> root, Edition edition,
			Constants constants ) {
		return injector( modulariser( edition, constants ).modularise( root ),
				Link.DEFAULE_CONSTRUCTION_STRATEGY, Link.BUILDIN );
	}

	public static Injector injector( Module[] modules, ConstructionStrategy strategy,
			Linker<Suppliable<?>> linker ) {
		return SuppliableInjector.create( linker.link( strategy, modules ) );
	}

	public static Modulariser modulariser( Edition edition, Constants constants ) {
		return new BuildinBootstrapper( edition, constants );
	}

	public static Bundler bundler( Edition edition, Constants constants ) {
		return new BuildinBootstrapper( edition, constants );
	}

	public static Suppliable<?>[] suppliables( Class<? extends Bundle> root ) {
		return Link.BUILDIN.link( Link.DEFAULE_CONSTRUCTION_STRATEGY, // 
				modulariser( Edition.FULL, Constants.NONE ).modularise( root ) );
	}

	private Bootstrap() {
		throw new UnsupportedOperationException( "util" );
	}

	public static void nonnullThrowsReentranceException( Object field ) {
		if ( field != null ) {
			throw new IllegalStateException( "Reentrance not allowed!" );
		}
	}

	public static <T extends Enum<T> & Feature<T>> Edition edition( T... featured ) {
		return new FeatureEdition<T>( featured[0], EnumSet.of( featured[0], featured ) );
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

	private static class BuildinBootstrapper
			implements Bootstrapper, Bundler, Modulariser {

		private final Map<Class<? extends Bundle>, Set<Class<? extends Bundle>>> bundleChildren = new IdentityHashMap<Class<? extends Bundle>, Set<Class<? extends Bundle>>>();
		private final Map<Class<? extends Bundle>, List<Module>> bundleModules = new IdentityHashMap<Class<? extends Bundle>, List<Module>>();
		private final Set<Class<? extends Bundle>> uninstalled = new HashSet<Class<? extends Bundle>>();
		private final Set<Class<? extends Bundle>> installed = new HashSet<Class<? extends Bundle>>();
		private final LinkedList<Class<? extends Bundle>> stack = new LinkedList<Class<? extends Bundle>>();
		private final Edition edition;
		private final Constants constants;

		BuildinBootstrapper( Edition edition, Constants constants ) {
			super();
			this.edition = edition;
			this.constants = constants;
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
		public <C extends Enum<C> & Const> void install( Class<? extends ModularBundle<C>> bundle,
				Class<C> property ) {
			if ( !edition.featured( property ) ) {
				return;
			}
			final C value = constants.value( property );
			TypeReflector.newInstance( bundle ).bootstrap( new ModularBootstrapper<C>() {

				@Override
				public void install( Class<? extends Bundle> bundle, C module ) {
					if ( module == value ) { // null is a valid value to define what happens when no configuration is present
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
			return (Class<? extends Bundle>[]) installed.toArray( new Class<?>[installed.size()] );
		}

		private Module[] modulesOf( Class<? extends Bundle>[] bundles ) {
			List<Module> installed = new ArrayList<Module>( bundles.length );
			for ( Class<? extends Bundle> b : bundles ) {
				List<Module> modules = bundleModules.get( b );
				if ( modules != null ) {
					installed.addAll( modules );
				}
			}
			return installed.toArray( new Module[installed.size()] );
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
