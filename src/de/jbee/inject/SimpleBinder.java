package de.jbee.inject;

/**
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @deprecated find a nicer solution for the core bootstrap
 */
@Deprecated
public class SimpleBinder
		implements Bindings {

	private final Bindings binder;
	private final Source source;
	private final Scope scope;

	public SimpleBinder( Bindings binder, Source source, Scope scope ) {
		super();
		this.binder = binder;
		this.source = source;
		this.scope = scope;
	}

	@Override
	public <T> void add( Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
			Source source ) {
		binder.add( resource, supplier, scope, source );
	}

	public <T> void wildcardBind( Class<T> type, Supplier<? extends T> supplier ) {
		Resource<T> resource = Instance.anyOf( Type.raw( type ).parametizedAsLowerBounds() ).toResource();
		add( resource, supplier, scope, source );
	}
}
