package se.jbee.inject.config;

import static se.jbee.inject.Type.raw;

import se.jbee.inject.Dependency;
import se.jbee.inject.Extension;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;

/**
 * {@link Plugins} are an {@link Extension} that makes resolving plugged
 * {@link Class}es for specific plugin points more convenient and formalise the
 * convention the mechanism is based upon.
 * 
 * @author Jan Bernitt
 * @since 19.1
 */
public final class Plugins implements Extension {

	/**
	 * The {@link Name} of the plugin-point.
	 * 
	 * @param point point class used as the main name-space
	 * @param property property name used as sub-space, use
	 *            {@link Class#getCanonicalName()} in case no specific property
	 *            should be targeted.
	 * @return A fully qualified plugin point name
	 */
	public static Name pluginPoint(Class<?> point, String property) {
		return Name.named(point.getCanonicalName() + ":" + property);
	}

	private final Injector context;
	private final Class<?> target;

	/**
	 * Called by the {@link Injector} itself when used as an {@link Extension}
	 */
	public Plugins(Injector context) {
		this(context, null);
	}

	private Plugins(Injector context, Class<?> target) {
		this.context = context;
		this.target = target;
	}

	public Plugins targeting(Class<?> target) {
		return new Plugins(context, target);
	}

	public Class<?>[] forPoint(Class<?> point) {
		return forPoint(point, Name.ANY.toString());
	}

	public Class<?>[] forPoint(Class<?> point, String property) {
		@SuppressWarnings("rawtypes")
		Dependency<Class[]> plugins = Dependency.dependency(
				raw(Class[].class)).named(pluginPoint(point, property));
		return context.resolve(
				target == null ? plugins : plugins.injectingInto(target));
	}
}
