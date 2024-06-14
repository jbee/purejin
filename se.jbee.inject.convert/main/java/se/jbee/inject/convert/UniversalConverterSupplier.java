package se.jbee.inject.convert;

import se.jbee.inject.*;
import se.jbee.inject.config.ConnectionTarget;
import se.jbee.inject.config.Connector;
import se.jbee.inject.config.Invoke;
import se.jbee.lang.Type;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static se.jbee.inject.Dependency.dependency;
import static se.jbee.lang.Type.actualParameterType;
import static se.jbee.lang.Type.actualReturnType;

/**
 *
 */
public class UniversalConverterSupplier
		implements Supplier<Converter<?, ?>>, Connector {

	private final Injector context;
	private final Map<Type<?>, Map<Type<?>, Converter<?, ?>>> directConverters = new ConcurrentHashMap<>();
	private final Map<Type<?>, ConvertTo<?>> chainConverters = new ConcurrentHashMap<>();

	public UniversalConverterSupplier(Injector context) {
		this.context = context;
	}

	@Override
	public Converter<?, ?> supply(Dependency<? super Converter<?, ?>> dep,
			Injector context) throws UnresolvableDependency {
		Type<?> converterType = dep.type();
		Converter<?, ?> res = resolve(converterType.parameter(0),
				converterType.parameter(1));
		if (res == null)
			throw new UnresolvableDependency.ResourceResolutionFailed(
					"No generic converter for", dep);
		return res;
	}

	@SuppressWarnings("unchecked")
	private <A, B> Converter<A, B> resolve(Type<A> from, Type<B> to) {
		Map<Type<?>, Converter<?, ?>> tos = directConverters.get(from);
		if (tos != null) {
			Converter<?, ?> converter = tos.get(to);
			if (converter != null)
				return (Converter<A, B>) converter;
		}
		ConvertTo<?> toChain = chainConverters.get(from);
		if (toChain != null) {
			return (Converter<A, B>) toChain.from(from);
		}
		return null;
	}

	@Override
	public void connect(Object instance, Type<?> as, Method conversion) {
		if (conversion.getParameterCount() != 1)
			return; // ignore for now
		int fromIndex = 0;
		if (conversion.isAnnotationPresent(Conversion.class))
			fromIndex = conversion.getAnnotation(Conversion.class).fromIndex();
		Type<?> from = actualParameterType(
				conversion.getParameters()[fromIndex], as);
		Type<?> to = actualReturnType(conversion, as);
		Invoke invoke = context.resolve(
				dependency(Invoke.class).injectingInto(as));
		ConnectionTarget target = new ConnectionTarget(instance, as, conversion,
				invoke);
		add(new ConversionSite<>(target, fromIndex, from, to, context,
				this::remove));
	}

	private void remove(ConversionSite<?, ?> site) {
		directConverters.compute(site.from, (key, value) -> {
			if (value == null)
				return null;
			value.remove(site.to, site);
			return value.isEmpty() ? null : value;
		});
	}

	public <A, B> void add(ConversionSite<A, B> site) {
		add(site.from, site.to, site, site.target.connected);
	}

	private <A, B> void add(Type<A> from, Type<B> to, Converter<A, B> converter,
			AnnotatedElement annotated) {
		directConverters.computeIfAbsent(from,
				key -> new ConcurrentHashMap<>()).put(to, converter);
		//TODO put chains
	}
}
