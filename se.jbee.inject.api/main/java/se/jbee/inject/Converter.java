package se.jbee.inject;

import se.jbee.lang.Type;

/**
 * General abstraction for value type conversion.
 *
 * @author Jan Bernitt
 *
 * @param <A> type of the input value
 * @param <B> type of the output value (converted value)
 */
@FunctionalInterface
public interface Converter<A, B> {

	static <A, B> Type<Converter<A, B>> converterTypeOf(Class<A> a, Class<B> b) {
		return converterTypeOf(Type.raw(a), Type.raw(b));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <A, B> Type<Converter<A, B>> converterTypeOf(Type<A> a, Type<B> b) {
		return (Type) Type.raw(Converter.class).parameterized(a, b);
	}

	/**
	 * Converts input to the corresponding value of the output type of this
	 * {@link Converter}.
	 *
	 * @param input any value including {@code null}
	 * @return output value including {@code null}
	 * @throws IllegalArgumentException In case the input value cannot be
	 *             converted to the output type
	 */
	B convert(A input);

	default <T> Converter<A, T> then(Converter<B, T> next) {
		return next.upon(this);
	}

	default <T> Converter<T, B> upon(Converter<T, A> prev) {
		return in -> convert(prev.convert(in));
	}

	default Converter<A, B> orElse(B constant) {
		return in -> {
			try {
				B res = convert(in);
				return res == null ? constant : res;
			} catch (IllegalArgumentException e) {
				return constant;
			}
		};
	}

}
