package se.jbee.inject.convert;

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
}
