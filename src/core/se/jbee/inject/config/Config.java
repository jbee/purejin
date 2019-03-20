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
import se.jbee.inject.Extension;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;

/**
 * A {@link Config} is an {@link Extension} that uses the {@link Dependency}
 * hierarchy to effectively name-space configuration values to avoid name
 * collisions.
 */
public class Config implements Extension {

	private final Injector context;
	private Instance<?> ns;

	public Config(Injector context) {
		this.context = context;
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

	public Optional<String> stringValue(String name) {
		return value(name, String.class);
	}

	public boolean booleanValue(String name) {
		return value(name, boolean.class).orElse(false);
	}

	public boolean booleanValue(String name, boolean defaultValue) {
		return value(name, boolean.class).orElse(defaultValue);
	}

	public int intValue(String name) {
		return value(name, int.class).orElse(0);
	}

	public int intValue(String name, int defaultValue) {
		return value(name, int.class).orElse(defaultValue);
	}

	public long longValue(String name) {
		return value(name, long.class).orElse(0L);
	}

	public long longValue(String name, long defaultValue) {
		return value(name, long.class).orElse(defaultValue);
	}

	public <E extends Enum<E>> E enumValue(String name, E defaultValue) {
		return value(name, defaultValue.getDeclaringClass()).orElse(
				defaultValue);
	}
}
