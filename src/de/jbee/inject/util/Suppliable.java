package de.jbee.inject.util;

import static de.jbee.inject.Demand.demand;
import static de.jbee.inject.Dependency.dependency;

import java.util.Arrays;

import de.jbee.inject.Demand;
import de.jbee.inject.Dependency;
import de.jbee.inject.Expiry;
import de.jbee.inject.Injectable;
import de.jbee.inject.Injector;
import de.jbee.inject.Injectron;
import de.jbee.inject.Precision;
import de.jbee.inject.Repository;
import de.jbee.inject.Resource;
import de.jbee.inject.Resourcing;
import de.jbee.inject.Source;
import de.jbee.inject.Supplier;

/**
 * Describing data of something that can be supplied.
 * 
 * It describes WHAT is supplied, HOW to supply it and how stable it is and WHERE it came from.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Suppliable<T>
		implements Resourcing<T> {

	public static InjectronSource source( Suppliable<?>[] suppliables ) {
		return new SuppliableSource( suppliables );
	}

	public final Resource<T> resource;
	public final Supplier<? extends T> supplier;
	public final Repository repository;
	public final Source source;
	public final Expiry expiry;

	public Suppliable( Resource<T> resource, Supplier<? extends T> supplier, Repository repository,
			Expiry expiry, Source source ) {
		super();
		this.resource = resource;
		this.supplier = supplier;
		this.repository = repository;
		this.expiry = expiry;
		this.source = source;
	}

	@Override
	public String toString() {
		return source + " / " + resource + " / " + supplier;
	}

	@Override
	public Resource<T> getResource() {
		return resource;
	}

	/**
	 * A {@link InjectronSource} that creates {@link Injectron}s from {@link Suppliable}s.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	private static class SuppliableSource
			implements InjectronSource {

		private final Suppliable<?>[] suppliables;

		SuppliableSource( Suppliable<?>[] suppliables ) {
			super();
			this.suppliables = suppliables;
		}

		@Override
		public Injectron<?>[] exportTo( Injector injector ) {
			return injectrons( suppliables, injector );
		}

		public static Injectron<?>[] injectrons( Suppliable<?>[] suppliables, Injector injector ) {
			final int total = suppliables.length;
			if ( total == 0 ) {
				return new Injectron<?>[0];
			}
			Arrays.sort( suppliables, Precision.RESOURCE_COMPARATOR );
			Injectron<?>[] res = new Injectron<?>[total];
			for ( int i = 0; i < total; i++ ) {
				res[i] = injectron( suppliables[i], injector, total, i );
			}
			return res;
		}

		private static <T> Injectron<T> injectron( Suppliable<T> s, Injector injector,
				int cardinality, int serialNumber ) {
			Resource<T> resource = s.resource;
			Dependency<T> dependency = dependency( resource.getInstance() );
			Demand<T> demand = demand( resource, dependency, serialNumber, cardinality );
			Injectable<T> injectable = Inject.asInjectable( s.supplier, injector );
			return Inject.injectorn( injectable, resource, demand, s.expiry, s.repository, s.source );
		}

	}
}
