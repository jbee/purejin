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

import se.jbee.inject.ContextAware;
import se.jbee.inject.Converter;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;

/**
 * A {@link Config} is an {@link Extension} that uses the {@link Dependency}
 * hierarchy to effectively name-space configuration values to avoid name
 * collisions.
 * 
 * @since 19.1
 */
public class Config implements ContextAware<Config>, Extension {

	private final Injector context;
	private final Instance<?> ns;

	/**
	 * Called by the {@link Injector} itself when used as an {@link Extension}
	 */
	public Config(Injector context) {
		this(context, null);
	}

	private Config(Injector context, Instance<?> ns) {
		this.context = context;
		this.ns = ns;
	}

	/**
	 * If {@link Config} is injected into other resources the name-space is
	 * automatically set
	 */
	@Override
	public Config inContext(Dependency<? super Config> context) {
		if (context.isUntargeted())
			return this;
		Instance<?> target = context.target();
		if (target.name.isAny())
			return of(target.type.rawType);
		return of(target);
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

	public final class Value<A> {

		private final String property;
		private final Class<A> from;

		private Value(String property, Class<A> from) {
			this.property = property;
			this.from = from;
		}

		public <B> Optional<B> as(Class<B> type) {
			Converter<A, B> converter = orElse(null,
					() -> Config.this.context.resolve(
							Converter.type(from, type)));
			if (converter == null)
				return empty();
			return Config.this.optionalValue(from, property) //
					.map(converter::convert);
		}

		public <B> B as(Class<B> type, B defaultValue) {
			return as(type).orElse(defaultValue);
		}
	}

	public Value<String> value(String property) {
		return value(String.class, property);
	}

	public <T> Value<T> value(Class<T> srcType, String property) {
		return new Value<>(property, srcType);
	}

	public <T> Optional<T> optionalValue(Class<T> type, String property) {
		return orElse(empty(), () -> ofNullable(
				context.resolve(toDependency(type, property))));
	}

	private <T> Dependency<T> toDependency(Class<T> type, String property) {
		Dependency<T> dep = dependency(type).named(property);
		if (ns != null)
			dep = dep.injectingInto(ns);
		dep = dep.injectingInto(Config.class);
		return dep;
	}

	public String stringValue(String property) {
		return stringValue(property, "");
	}

	public String stringValue(String property, String defaultValue) {
		return optionalValue(String.class, property).orElse(defaultValue);
	}

	public boolean booleanValue(String property) {
		return booleanValue(property, false);
	}

	public boolean booleanValue(String property, boolean defaultValue) {
		return optionalValue(boolean.class, property).orElse(defaultValue);
	}

	public int intValue(String property) {
		return intValue(property, 0);
	}

	public int intValue(String property, int defaultValue) {
		return optionalValue(int.class, property).orElse(defaultValue);
	}

	public long longValue(String property) {
		return longValue(property, 0L);
	}

	public long longValue(String property, long defaultValue) {
		return optionalValue(long.class, property).orElse(defaultValue);
	}

	public float floatValue(String property) {
		return floatValue(property, 0f);
	}

	public float floatValue(String property, float defaultValue) {
		return optionalValue(float.class, property).orElse(defaultValue);
	}

	public double doubleValue(String property) {
		return doubleValue(property, 0d);
	}

	public double doubleValue(String property, double defaultValue) {
		return optionalValue(double.class, property).orElse(defaultValue);
	}

	public <E extends Enum<E>> E enumValue(String property, E defaultValue) {
		return optionalValue(defaultValue.getDeclaringClass(), property) //
				.orElse(defaultValue);
	}
}
