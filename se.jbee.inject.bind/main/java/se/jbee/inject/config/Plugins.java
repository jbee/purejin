package se.jbee.inject.config;

import se.jbee.inject.ContextAware;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.lang.Type.raw;

/**
 * {@link Plugins} are an {@link Extension} that makes resolving plugged
 * {@link Class}es for specific plugin points more convenient and formalise the
 * convention of the mechanism.
 *
 * @since 8.1
 */
public final class Plugins implements ContextAware<Plugins>, Extension {

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
		return property.isEmpty()
				? Name.named(point)
				: Name.named(property).in(Name.named(point));
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

	@Override
	public Plugins inContext(Dependency<? super Plugins> context) {
		if (context.isNotTargeted())
			return target == null ? this : targeting(null);
		return targeting(context.target().type.rawType);
	}

	public Plugins targeting(Class<?> target) {
		return new Plugins(context, target);
	}

	public Class<?>[] forPoint(Class<?> point) {
		return forPoint(point, Name.ANY.toString());
	}

	public Class<?>[] forPoint(Class<?> point, String property) {
		@SuppressWarnings("rawtypes")
		Dependency<Class[]> plugins = dependency(
				raw(Class[].class)).named(pluginPoint(point, property));
		return context.resolve(
				target == null ? plugins : plugins.injectingInto(target));
	}

	public Class<?> getTarget() {
		return target;
	}
}
