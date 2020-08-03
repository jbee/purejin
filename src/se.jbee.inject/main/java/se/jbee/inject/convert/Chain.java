package se.jbee.inject.convert;

import static se.jbee.inject.Instance.defaultInstanceOf;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import se.jbee.inject.Converter;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Type;

public final class Chain<B> {

	static final class Link<A, B> {
		final Type<A> in;
		final Type<B> out;
		final Converter<A, B> converter;

		Link(Type<A> in, Type<B> out, Converter<A, B> converter) {
			this.in = in;
			this.out = out;
			this.converter = converter;
		}
	}

	private final Type<B> outputType;
	private final Map<Type<?>, Converter<?, B>> headsByInputType = new LinkedHashMap<>();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Chain(Converter<?, B> tail, Injector context) {
		Class<? extends Converter> tailType = tail.getClass();
		Type<? extends Converter> converterType = Type.supertype(
				Converter.class, Type.raw(tailType));
		Type<?> directInputType = converterType.parameter(0);
		this.outputType = (Type) converterType.parameter(1);
		headsByInputType.put(directInputType, tail);
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
				Converter<?, B> chainBuilder = headsByInputType.get(
						directInputType);
				while (tail != null) {
					@SuppressWarnings("rawtypes")
					Converter converter = tail.converter;
					chainBuilder = chainBuilder.after(converter);
					headsByInputType.put(tail.in, chainBuilder);
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
				context.resolve(out.name, Converter.type(in, out.type)));
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

	public <A> Converter<A, B> forInput(Type<A> type) {
		@SuppressWarnings("unchecked")
		Converter<A, B> res = (Converter<A, B>) headsByInputType.get(type);
		if (res == null)
			throw new UnsupportedOperationException(
					"Conversion from " + type + "not available");
		return res;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		for (Type<?> in : headsByInputType.keySet()) {
			str.append(in).append(" => ").append(outputType).append('\n');
		}
		return str.toString();
	}

}
