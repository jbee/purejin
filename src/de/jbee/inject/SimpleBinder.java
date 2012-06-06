package de.jbee.inject;

public class SimpleBinder
		implements BindDeclarator {

	private final BindDeclarator binder;
	private final Source source;
	private final Scope scope;

	public SimpleBinder( BindDeclarator binder, Source source, Scope scope ) {
		super();
		this.binder = binder;
		this.source = source;
		this.scope = scope;
	}

	@Override
	public <T> void bind( Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
			Source source ) {
		binder.bind( resource, supplier, scope, source );
	}

	public <T> void wildcardBind( Class<T> type, Supplier<? extends T> supplier ) {
		Resource<T> resource = Instance.anyOf( Type.raw( type ).parametizedAsLowerBounds() ).toResource();
		bind( resource, supplier, scope, source );
	}
}
