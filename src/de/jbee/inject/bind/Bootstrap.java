package de.jbee.inject.bind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.jbee.inject.Expiry;
import de.jbee.inject.Injector;
import de.jbee.inject.Precision;
import de.jbee.inject.Repository;
import de.jbee.inject.Resource;
import de.jbee.inject.Scope;
import de.jbee.inject.Source;
import de.jbee.inject.Suppliable;
import de.jbee.inject.SuppliableInjector;
import de.jbee.inject.Supplier;
import de.jbee.inject.util.Scoped;
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
		return injector( root, new BuildinInstaller( edition, constants ) );
	}

	public static Injector injector( Class<? extends Bundle> root, Installer installer ) {
		return SuppliableInjector.create( installer.install( root ) );
	}

	private Bootstrap() {
		// util class
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

	private static class BuildinInstaller
			implements Installer {

		// Find the initial set of bindings
		// 0. create BindInstruction
		// 2. sort declarations
		// 3. remove duplicates (implicit will be sorted after explicit)
		// 4. detect ambiguous bindings (two explicit bindings that have same type and target)

		// 1. Create Scope-Repositories
		//   a. sort scopes from most stable to most fragile
		// 	 b. init one repository for each scope
		// 	 c. apply snapshots wrapper to repository instances

		private final Edition edition;
		private final Constants constants;

		BuildinInstaller( Edition edition, Constants constants ) {
			super();
			this.edition = edition;
			this.constants = constants;
		}

		@Override
		public Suppliable<?>[] install( Class<? extends Bundle> root ) {
			return install( cleanedUp( bindingsFrom( root, edition ) ) );
		}

		private Suppliable<?>[] install( Binding<?>[] bindings ) {
			Map<Scope, Repository> repositories = initRepositories( bindings );
			Suppliable<?>[] suppliables = new Suppliable<?>[bindings.length];
			//TODO
			Expiry expiration = Expiry.NEVER;
			for ( int i = 0; i < bindings.length; i++ ) {
				Binding<?> binding = bindings[i];
				Scope scope = binding.scope;
				suppliables[i] = binding.suppliableIn( repositories.get( scope ),
						Expiry.expires( scope == Scoped.INJECTION
							? 1
							: 0 ) );
			}
			return suppliables;
		}

		private Map<Scope, Repository> initRepositories( Binding<?>[] bindings ) {
			Map<Scope, Repository> repositories = new IdentityHashMap<Scope, Repository>();
			for ( Binding<?> i : bindings ) {
				Repository repository = repositories.get( i.scope );
				if ( repository == null ) {
					repositories.put( i.scope, i.scope.init() );
				}
			}
			return repositories;
		}

		private Binding<?>[] cleanedUp( Binding<?>[] bindings ) {
			if ( bindings.length <= 1 ) {
				return bindings;
			}
			List<Binding<?>> res = new ArrayList<Binding<?>>( bindings.length );
			Arrays.sort( bindings );
			res.add( bindings[0] );
			int lastIndependend = 0;
			for ( int i = 1; i < bindings.length; i++ ) {
				Binding<?> one = bindings[lastIndependend];
				Binding<?> other = bindings[i];
				boolean equalResource = one.resource.equalTo( other.resource );
				if ( !equalResource || !other.source.getType().replacedBy( one.source.getType() ) ) {
					res.add( other );
					lastIndependend = i;
				} else if ( one.source.getType().clashesWith( other.source.getType() ) ) {
					throw new IllegalStateException( "Duplicate binds:" + one + "," + other );
				}
			}
			return res.toArray( new Binding[res.size()] );
		}

		private Binding<?>[] bindingsFrom( Class<? extends Bundle> root, Edition edition ) {
			BuildinBootstrapper bootstrapper = new BuildinBootstrapper( edition, constants );
			bootstrapper.install( root );
			ListBindings bindings = new ListBindings();
			for ( Module m : bootstrapper.installed( root ) ) {
				m.declare( bindings );
			}
			return bindings.list.toArray( new Binding<?>[0] );
		}

	}

	private static class ListBindings
			implements Bindings {

		final List<Binding<?>> list = new LinkedList<Binding<?>>();

		ListBindings() {
			// make visible
		}

		@Override
		public <T> void add( Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
				Source source ) {
			list.add( new Binding<T>( resource, supplier, scope, source ) );
		}

	}

	private static final class Binding<T>
			implements Comparable<Binding<?>> {

		final Resource<T> resource;
		final Supplier<? extends T> supplier;
		final Scope scope;
		final Source source;

		Binding( Resource<T> resource, Supplier<? extends T> supplier, Scope scope, Source source ) {
			super();
			this.resource = resource;
			this.supplier = supplier;
			this.scope = scope;
			this.source = source;
		}

		@Override
		public int compareTo( Binding<?> other ) {
			int res = Precision.comparePrecision( resource.getInstance(),
					other.resource.getInstance() );
			if ( res != 0 ) {
				return res;
			}
			//TODO what about the Availability ? 
			res = Precision.comparePrecision( source, other.source );
			if ( res != 0 ) {
				return res;
			}
			return -1; // keep order
		}

		@Override
		public String toString() {
			return source + " / " + resource + " / " + scope;
		}

		Suppliable<T> suppliableIn( Repository repository, Expiry expiration ) {
			return new Suppliable<T>( resource, supplier, repository, expiration, source );
		}

	}

	private static class BuildinBootstrapper
			implements Bootstrapper, ModuleTree {

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
		public Module[] installed( Class<? extends Bundle> root ) {
			Set<Class<? extends Bundle>> installed = new LinkedHashSet<Class<? extends Bundle>>();
			allInstalledIn( root, installed );
			return modulesOf( installed );
		}

		private Module[] modulesOf( Set<Class<? extends Bundle>> bundles ) {
			List<Module> installed = new ArrayList<Module>( bundles.size() );
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

		private void allInstalledIn( Class<? extends Bundle> bundle,
				Set<Class<? extends Bundle>> accu ) {
			accu.add( bundle );
			Set<Class<? extends Bundle>> children = bundleChildren.get( bundle );
			if ( children == null ) {
				return;
			}
			for ( Class<? extends Bundle> c : children ) {
				if ( !accu.contains( c ) ) {
					allInstalledIn( c, accu );
				}
			}
		}

	}

}
