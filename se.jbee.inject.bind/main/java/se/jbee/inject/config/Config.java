package se.jbee.inject.config;

import se.jbee.inject.*;
import se.jbee.lang.Type;

import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.raw;
import static se.jbee.lang.Utils.orElse;

/**
 * The {@link Config} is a runtime-configuration of the application. It allows
 * other "external" software modules to contribute to the configuration of other
 * software components.
 * <p>
 * A {@link Config} is an {@link Extension} that uses the {@link Dependency}
 * hierarchy to effectively name-space configuration values to avoid name
 * collisions.
 *
 * As it is {@link ContextAware} it can be injected into the configured bean
 * already being scoped to the target, that means only the configurations for
 * the target instance are "in scope" or visible.
 *
 * @since 8.1
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
		if (context.isNotTargeted())
			return ns == null ? this : new Config(this.context, null);
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
		private final Type<A> from;

		private Value(String property, Type<A> from) {
			this.property = property;
			this.from = from;
		}

		public <B> Optional<B> as(Class<B> type) {
			return as(raw(type));
		}

		public <B> Optional<B> as(Type<B> type) {
			if (from.isAssignableTo(type))
				return (Optional<B>) optionalValue(from, property);
			Converter<A, B> converter = orElse(null,
					() -> Config.this.context.resolve(
							Converter.converterTypeOf(from, type)));
			if (converter == null)
				return empty();
			return Config.this.optionalValue(from, property) //
					.map(converter::convert);
		}

		public Source source() {
			Dependency<A> dep = toDependency(from, property);
			Resource<?> r = context.resolve(dep.typed(Resource.resourceTypeOf(dep.type())));
			return r.source;
		}

		public <B> B as(Class<B> type, B defaultValue) {
			return as(type).orElse(defaultValue);
		}
	}

	public Value<String> value(String property) {
		return value(String.class, property);
	}

	public <T> Value<T> value(Class<T> srcType, String property) {
		return value(raw(srcType), property);
	}

	public <T> Value<T> value(Type<T> srcType, String property) {
		return new Value<>(property, srcType);
	}

	public <T> Optional<T> optionalValue(Class<T> type, String property) {
		return optionalValue(raw(type), property);
	}

	public <T> Optional<T> optionalValue(Type<T> type, String property) {
		return orElse(empty(), () -> ofNullable(
				context.resolve(toDependency(type, property))));
	}

	private <T> Dependency<T> toDependency(Type<T> type, String property) {
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
