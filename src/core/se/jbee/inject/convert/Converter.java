package se.jbee.inject.convert;

import se.jbee.inject.Type;

/**
 * 
 * @author Jan Bernitt
 *
 * @param <A> type of the input value
 * @param <B> type of the output value (converted value)
 */
public interface Converter<A, B> {

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

	default <T> Converter<A, T> andThen(Converter<B, T> next) {
		return next.after(this);
	}

	default <T> Converter<T, B> after(Converter<T, A> prev) {
		return in -> convert(prev.convert(in));
	}

	default Converter<A, B> orOnError(B returns) {
		return in -> {
			try {
				return convert(in);
			} catch (IllegalArgumentException e) {
				return returns;
			}
		};
	}

	static <A, B> Type<Converter<A, B>> type(Class<A> a, Class<B> b) {
		return type(Type.raw(a), Type.raw(b));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <A, B> Type<Converter<A, B>> type(Type<A> a, Type<B> b) {
		return (Type) Type.raw(Converter.class).parametized(a, b);
	}

}
