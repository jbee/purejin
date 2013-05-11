/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.util;

import static se.jbee.inject.Demand.demand;
import static se.jbee.inject.Dependency.dependency;

import java.util.Arrays;

import se.jbee.inject.Demand;
import se.jbee.inject.Dependency;
import se.jbee.inject.Expiry;
import se.jbee.inject.Injectable;
import se.jbee.inject.Injector;
import se.jbee.inject.Injectron;
import se.jbee.inject.Precision;
import se.jbee.inject.Repository;
import se.jbee.inject.Resource;
import se.jbee.inject.Resourced;
import se.jbee.inject.Source;
import se.jbee.inject.Supplier;

/**
 * Describing data of something that can be supplied.
 * 
 * It describes WHAT is supplied, HOW to supply it and how stable it is and WHERE it came from.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Suppliable<T>
		implements Resourced<T> {

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
	 * @author Jan Bernitt (jan@jbee.se)
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
			return Inject.injectron( injectable, resource, demand, s.expiry, s.repository, s.source );
		}

	}
}
