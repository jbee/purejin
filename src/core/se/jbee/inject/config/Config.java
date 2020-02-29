package se.jbee.inject.config;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.orElse;

import java.util.Optional;

import se.jbee.inject.Dependency;
import se.jbee.inject.SPI;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;

/**
 * A {@link Config} is an {@link SPI} that uses the {@link Dependency}
 * hierarchy to effectively name-space configuration values to avoid name
 * collisions.
 * 
 * @since 19.1
 */
public class Config implements SPI {

	private final Injector context;
	private final Instance<?> ns;

	/**
	 * Called by the {@link Injector} itself when used as an {@link SPI}
	 */
	public Config(Injector context) {
		this(context, null);
	}

	private Config(Injector context, Instance<?> ns) {
		this.context = context;
		this.ns = ns;
	}

	public Config of(Class<?> ns) {
		return of(defaultInstanceOf(raw(ns)));
	}

	public Config of(String instance, Class<?> ns) {
		return of(instance(named(instance), raw(ns)));
	}

	public Config of(Instance<?> ns) {
		return new Config(context, ns);
	}

	public <T> Optional<T> value(String name, Class<T> type) {
		return orElse(empty(), () -> {
			Dependency<T> dep = dependency(type).named(name);
			if (ns != null)
				dep = dep.injectingInto(ns);
			dep = dep.injectingInto(Config.class);
			return ofNullable(context.resolve(dep));
		});
	}

	public String stringValue(String name) {
		return stringValue(name, "");
	}

	public String stringValue(String name, String defaultValue) {
		return value(name, String.class).orElse(defaultValue);
	}

	public boolean booleanValue(String name) {
		return booleanValue(name, false);
	}

	public boolean booleanValue(String name, boolean defaultValue) {
		return value(name, boolean.class).orElse(defaultValue);
	}

	public int intValue(String name) {
		return intValue(name, 0);
	}

	public int intValue(String name, int defaultValue) {
		return value(name, int.class).orElse(defaultValue);
	}

	public long longValue(String name) {
		return longValue(name, 0L);
	}

	public long longValue(String name, long defaultValue) {
		return value(name, long.class).orElse(defaultValue);
	}

	public float floatValue(String name) {
		return floatValue(name, 0f);
	}

	public float floatValue(String name, float defaultValue) {
		return value(name, float.class).orElse(defaultValue);
	}

	public double doubleValue(String name) {
		return doubleValue(name, 0d);
	}

	public double doubleValue(String name, double defaultValue) {
		return value(name, double.class).orElse(defaultValue);
	}

	public <E extends Enum<E>> E enumValue(String name, E defaultValue) {
		return value(name, defaultValue.getDeclaringClass()).orElse(
				defaultValue);
	}
}
