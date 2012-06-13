package de.jbee.inject.bind;

import static de.jbee.inject.PreciserThanComparator.comparePrecision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Provider;
import de.jbee.inject.Repository;
import de.jbee.inject.Resource;
import de.jbee.inject.Scope;
import de.jbee.inject.Source;
import de.jbee.inject.Supplier;
import de.jbee.inject.Suppliers;
import de.jbee.inject.TypeReflector;

public class Bootstrap {

	public static DependencyResolver injector( Class<? extends Bundle> root ) {
		return BindableInjector.create( root, new BuildinBundleBinder() );
	}

	public static void install( Bootstrapper bs, CoreModule... modules ) {
		for ( CoreModule m : modules ) {
			bs.install( m.bundle );
		}
	}

	public static enum CoreModule {
		PROVIDER( BuildinProviderModule.class ),
		LIST( BuildinListModule.class ),
		SET( BuildinSetModule.class );

		final Class<? extends Bundle> bundle;

		private CoreModule( Class<? extends Bundle> bundle ) {
			this.bundle = bundle;
		}

	}

	static final class BindDeclaration<T>
			implements Comparable<BindDeclaration<?>> {

		private final int nr;
		private final Resource<T> resource;
		private final Supplier<? extends T> supplier;
		private final Scope scope;
		private final Source source;

		BindDeclaration( int nr, Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
				Source source ) {
			super();
			this.nr = nr;
			this.resource = resource;
			this.supplier = supplier;
			this.scope = scope;
			this.source = source;
		}

		@Override
		public int compareTo( BindDeclaration<?> other ) {
			int res = comparePrecision( resource.getInstance(), other.resource.getInstance() );
			if ( res != 0 ) {
				return res;
			}
			//TODO what about the Availability ? 
			res = comparePrecision( source, other.source );
			if ( res != 0 ) {
				return res;
			}
			return Integer.valueOf( nr ).compareTo( other.nr );
		}

		@Override
		public String toString() {
			return source + " / " + resource + " / " + scope;
		}

		Resource<T> resource() {
			return resource;
		}

		Scope scope() {
			return scope;
		}

		Source source() {
			return source;
		}

		Supplier<? extends T> supplier() {
			return supplier;
		}

		Binding<T> toBinding( Repository repository ) {
			return new Binding<T>( resource, supplier, repository, source );
		}

	}

	static class BuildinBootstrapper
			implements Bootstrapper {

		private final Map<Class<? extends Bundle>, Set<Class<? extends Bundle>>> bundleChildren = new IdentityHashMap<Class<? extends Bundle>, Set<Class<? extends Bundle>>>();
		private final Map<Class<? extends Bundle>, List<Module>> bundleModules = new IdentityHashMap<Class<? extends Bundle>, List<Module>>();
		private final Set<Class<? extends Bundle>> uninstalled = new HashSet<Class<? extends Bundle>>();
		private final LinkedList<Class<? extends Bundle>> stack = new LinkedList<Class<? extends Bundle>>();

		@Override
		public void install( Class<? extends Bundle> bundle ) {
			if ( uninstalled.contains( bundle ) ) {
				return;
			}
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
		public void install( Module module ) {
			Class<? extends Bundle> bundle = stack.peek();
			if ( uninstalled.contains( bundle ) ) {
				return;
			}
			List<Module> modules = bundleModules.get( bundle );
			if ( modules == null ) {
				modules = new ArrayList<Module>();
				bundleModules.put( bundle, modules );
			}
			modules.add( module );
		}

		public List<Module> installed( Class<? extends Bundle> root ) {
			Set<Class<? extends Bundle>> installed = new LinkedHashSet<Class<? extends Bundle>>();
			allInstalledIn( root, installed );
			return modulesOf( installed );
		}

		public List<Module> modulesOf( Set<Class<? extends Bundle>> bundles ) {
			List<Module> installed = new ArrayList<Module>( bundles.size() );
			for ( Class<? extends Bundle> b : bundles ) {
				List<Module> modules = bundleModules.get( b );
				if ( modules != null ) {
					installed.addAll( modules );
				}
			}
			return installed;
		}

		@Override
		public void uninstall( Class<? extends Bundle> bundle ) {
			if ( uninstalled.contains( bundle ) ) {
				return;
			}
			for ( Set<Class<? extends Bundle>> c : bundleChildren.values() ) {
				c.remove( bundle );
			}
			bundleModules.remove( bundle ); // we are sure we don't need its modules
			uninstalled.add( bundle );
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

	static class BuildinBundleBinder
			implements BundleBinder {

		// Find the initial set of bindings
		// 0. create BindInstruction
		// 2. sort declarations
		// 3. remove duplicates (implicit will be sorted after explicit)
		// 4. detect ambiguous bindings (two explicit bindings that have same type and availability)

		// 1. Create Scope-Repositories
		//   a. sort scopes from most stable to most fragile
		// 	 b. init one repository for each scope
		// 	 c. apply snapshots wrapper to repository instances
		@Override
		public Binding<?>[] install( Class<? extends Bundle> root ) {
			return bind( cleanedUp( declarationsFrom( root ) ) );
		}

		private Binding<?>[] bind( BindDeclaration<?>[] declarations ) {
			Map<Scope, Repository> repositories = buildRepositories( declarations );
			Binding<?>[] bindings = new Binding<?>[declarations.length];
			for ( int i = 0; i < declarations.length; i++ ) {
				BindDeclaration<?> instruction = declarations[i];
				bindings[i] = instruction.toBinding( repositories.get( instruction.scope() ) );
			}
			return bindings;
		}

		private Map<Scope, Repository> buildRepositories( BindDeclaration<?>[] declarations ) {
			Map<Scope, Repository> repositories = new IdentityHashMap<Scope, Repository>();
			for ( BindDeclaration<?> i : declarations ) {
				Repository repository = repositories.get( i.scope() );
				if ( repository == null ) {
					repositories.put( i.scope(), i.scope().init( declarations.length ) );
				}
			}
			return repositories;
		}

		private BindDeclaration<?>[] cleanedUp( BindDeclaration<?>[] declarations ) {
			if ( declarations.length == 0 ) {
				return declarations;
			}
			List<BindDeclaration<?>> res = new ArrayList<BindDeclaration<?>>( declarations.length );
			Arrays.sort( declarations );
			for ( BindDeclaration<?> d : declarations ) {
				//TODO filter
				res.add( d );
			}
			return res.toArray( new BindDeclaration[res.size()] );
		}

		private BindDeclaration<?>[] declarationsFrom( Class<? extends Bundle> root ) {
			BuildinBootstrapper binder = new BuildinBootstrapper();
			binder.install( root );
			SimpleBindings bindings = new SimpleBindings();
			for ( Module m : binder.installed( root ) ) {
				m.configure( bindings );
			}
			return bindings.declarations.toArray( new BindDeclaration<?>[0] );
		}
	}

	static class SimpleBindings
			implements Bindings {

		final List<BindDeclaration<?>> declarations = new LinkedList<BindDeclaration<?>>();

		@Override
		public <T> void add( Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
				Source source ) {
			declarations.add( new BindDeclaration<T>( declarations.size(), resource, supplier,
					scope, source ) );
		}

	}

	private static final class BuildinListModule
			extends PackageModule {

		@Override
		public void configure() {
			superbind( List.class ).to( Suppliers.LIST_BRIDGE );
		}

	}

	/**
	 * Installs all the build-in functionality by using the core API.
	 */
	private static final class BuildinProviderModule
			extends PackageModule {

		@Override
		protected void configure() {
			superbind( Provider.class ).to( Suppliers.PROVIDER );
		}

	}

	private static final class BuildinSetModule
			extends PackageModule {

		@Override
		public void configure() {
			superbind( Set.class ).to( Suppliers.SET_BRIDGE );
		}

	}
}
