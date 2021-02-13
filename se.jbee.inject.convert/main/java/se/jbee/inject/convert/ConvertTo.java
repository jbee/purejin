package se.jbee.inject.convert;

import se.jbee.inject.Converter;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.lang.Type;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.lang.Type.raw;

/**
 * A {@link ConvertTo} groups {@link Converter}s that all convert to the same
 * output type from different input types.
 * <p>
 * It makes use of {@link Converter} chains to allow for as many input types
 * based on the know type conversions.
 *
 * @param <B> output type of the conversion
 */
public final class ConvertTo<B> {

	static final class Link<A, B> {
		final Type<A> in;
		final Type<B> out;
		final Converter<A, B> by;

		Link(Type<A> in, Type<B> out, Converter<A, B> by) {
			this.in = in;
			this.out = out;
			this.by = by;
		}
	}

	private final Type<B> out;
	private final Map<Type<?>, Converter<?, B>> ins = new LinkedHashMap<>();


	@SuppressWarnings({"rawtypes", "unchecked"})
	public ConvertTo(Converter<?, B> tail, Injector context) {
		Class<? extends Converter> tailType = tail.getClass();
		Type<?> converterType = raw(
				tailType).toSuperType(Converter.class);
		Type<?> directInputType = converterType.parameter(0);
		this.out = (Type) converterType.parameter(1);
		ins.put(directInputType, tail);
		initAnnotatedChains(tailType, directInputType, context);
	}

	@SuppressWarnings("unchecked")
	private void initAnnotatedChains(Class<?> tailType, Type<?> directInputType,
			Injector context) {
		Imported imports = Imported.base().add(
				tailType.getAnnotation(Imports.class));
		if (tailType.isAnnotationPresent(Converts.class)) {
			for (Converts converts : tailType.getAnnotationsByType(
					Converts.class)) {
				Imported localImports = imports;
				if (converts.imports().length > 0)
					localImports = imports.fork().add(converts.imports());
				Instance<?>[] chain = instanceChain(localImports,
						converts.value());
				Deque<Link<?, ?>> converterChain = converterChain(chain,
						directInputType, context);
				Link<?, ?> tail = converterChain.pollLast();
				Converter<?, B> chainBuilder = ins.get(
						directInputType);
				while (tail != null) {
					@SuppressWarnings("rawtypes")
					Converter converter = tail.by;
					chainBuilder = chainBuilder.upon(converter);
					ins.put(tail.in, chainBuilder);
					tail = converterChain.pollLast();
				}
			}
		}
	}

	static Deque<Link<?, ?>> converterChain(Instance<?>[] chain,
			Type<?> directInputType, Injector context) {
		LinkedList<Link<?, ?>> converterChain = new LinkedList<>();
		for (int i = 1; i < chain.length; i++) {
			Instance<?> in = chain[i - 1];
			Instance<?> out = chain[i];
			converterChain.add(createLink(in.type, out, context));
		}
		Type<?> in = chain[chain.length - 1].type;
		Type<?> out = directInputType;
		if (!in.equalTo(out)) {
			converterChain.add(createLink(in, defaultInstanceOf(out), context));
		}
		return converterChain;
	}

	private static <A, B> Link<A, B> createLink(Type<A> in, Instance<B> out,
			Injector context) {
		return new Link<>(in, out.type,
				context.resolve(out.name, Converter.converterTypeOf(in, out.type)));
	}

	static Instance<?>[] instanceChain(Imported imports, String[] chain) {
		Instance<?>[] chainInstances = new Instance[chain.length];
		for (int i = 0; i < chain.length; i++) {
			if (chain[i].indexOf(' ') >= 0) {
				String[] nameAndType = chain[i].split("\\s+");
				chainInstances[i] = Instance.instance(
						Name.named(nameAndType[0]),
						imports.resolve(nameAndType[1]));
			} else {
				chainInstances[i] = Instance.anyOf(imports.resolve(chain[i]));
			}
		}
		return chainInstances;
	}

	public <A> Converter<A, B> from(Type<A> in) {
		@SuppressWarnings("unchecked")
		Converter<A, B> res = (Converter<A, B>) ins.get(in);
		if (res == null)
			throw new UnsupportedOperationException(
					"Conversion from " + in + "not available");
		return res;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		for (Type<?> in : ins.keySet()) {
			str.append(in).append(" => ").append(out).append('\n');
		}
		return str.toString();
	}

}
