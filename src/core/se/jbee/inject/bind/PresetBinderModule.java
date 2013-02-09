package se.jbee.inject.bind;

public abstract class PresetBinderModule<T>
		extends AbstractBinderModule
		implements Bundle, PresetModule<T> {

	@Override
	public final void bootstrap( Bootstrapper bootstrap ) {
		bootstrap.install( this );
	}

	@Override
	public final void declare( Bindings bindings, Inspector inspector, T preset ) {
		init( bindings, inspector );
		declare( preset );
	}

	protected abstract void declare( T preset );
}
