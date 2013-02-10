package se.jbee.inject.bind;

/**
 * The default utility {@link PresetModule}.
 * 
 * A {@link BinderModuleWith} is also a {@link Bundle} so it should be used and installed as such.
 * It will than {@link Bundle#bootstrap(Bootstrapper)} itself as a module.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class BinderModuleWith<T>
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

	/**
	 * @see PresetModule#declare(Bindings, Inspector, Object)
	 * @param preset
	 *            The value contained in the {@link Presets} for the type of this
	 *            {@link PresetModule}.
	 */
	protected abstract void declare( T preset );
}