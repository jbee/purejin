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

import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Precision;
import de.jbee.inject.Repository;
import de.jbee.inject.Resource;
import de.jbee.inject.Scope;
import de.jbee.inject.Source;
import de.jbee.inject.Supplier;
import de.jbee.inject.TypeReflector;

public final class Bootstrap {

	public static DependencyResolver injector( Class<? extends Bundle> root ) {
		return BindableInjector.create( root, new BuildinBundleBinder() );
	}

	private Bootstrap() {
		// util class
	}

	public static void nonnullThrowsReentranceException( Object field ) {
		if ( field != null ) {
			throw new IllegalStateException( "Reentrance not allowed!" );
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
					repositories.put( i.scope(), i.scope().init() );
				}
			}
			return repositories;
		}

		private BindDeclaration<?>[] cleanedUp( BindDeclaration<?>[] declarations ) {
			if ( declarations.length <= 1 ) {
				return declarations;
			}
			List<BindDeclaration<?>> res = new ArrayList<BindDeclaration<?>>( declarations.length );
			Arrays.sort( declarations );
			res.add( declarations[0] );
			for ( int i = 1; i < declarations.length; i++ ) {
				if ( independent( declarations[i], declarations[i - 1] ) ) {
					res.add( declarations[i] );
				}
			}
			return res.toArray( new BindDeclaration[res.size()] );
		}

		private BindDeclaration<?>[] declarationsFrom( Class<? extends Bundle> root ) {
			BuildinBootstrapper bootstrapper = new BuildinBootstrapper();
			bootstrapper.install( root );
			ListBindings bindings = new ListBindings();
			for ( Module m : bootstrapper.installed( root ) ) {
				m.declare( bindings );
			}
			return bindings.declarations.toArray( new BindDeclaration<?>[0] );
		}

		private boolean independent( BindDeclaration<?> one, BindDeclaration<?> other ) {
			if ( one.resource().includes( other.resource() ) ) {
				if ( one.source().isImplicit() ) {
					return false;
				}
			}
			return true;
		}
	}

	static class ListBindings
			implements Bindings {

		final List<BindDeclaration<?>> declarations = new LinkedList<BindDeclaration<?>>();

		@Override
		public <T> void add( Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
				Source source ) {
			declarations.add( new BindDeclaration<T>( declarations.size(), resource, supplier,
					scope, source ) );
		}

	}

	private static final class BindDeclaration<T>
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

		Binding<T> toBinding( Repository repository ) {
			return new Binding<T>( resource, supplier, repository, source );
		}

	}

	private static class BuildinBootstrapper
			implements Bootstrapper {

		private final Map<Class<? extends Bundle>, Set<Class<? extends Bundle>>> bundleChildren = new IdentityHashMap<Class<? extends Bundle>, Set<Class<? extends Bundle>>>();
		private final Map<Class<? extends Bundle>, List<Module>> bundleModules = new IdentityHashMap<Class<? extends Bundle>, List<Module>>();
		private final Set<Class<? extends Bundle>> uninstalled = new HashSet<Class<? extends Bundle>>();
		private final Set<Class<? extends Bundle>> installed = new HashSet<Class<? extends Bundle>>();
		private final LinkedList<Class<? extends Bundle>> stack = new LinkedList<Class<? extends Bundle>>();

		BuildinBootstrapper() {
			// make visible
		}

		@Override
		public void install( Class<? extends Bundle> bundle ) {
			if ( uninstalled.contains( bundle ) || installed.contains( bundle ) ) {
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
		public <M extends Enum<M> & ModularBundle<M>> void install( M... modules ) {
			if ( modules.length > 0 ) {
				final EnumSet<M> installing = EnumSet.of( modules[0], modules );
				modules[0].bootstrap( new ModularBootstrapper<M>() {

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
