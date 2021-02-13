package se.jbee.inject.convert;

import se.jbee.lang.Type;

import java.io.Closeable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * <p>
 * A {@link Imported} is the dynamic equivalent of a Java source file's
 * <code>import</code> context which allows to use simple names instead of fully
 * qualified class names.
 * </p>
 * <p>
 * The utility allows to resolve {@link Type}s from {@link String}s with simple
 * type names using {@link #resolve(String)}. To expand the simple name a import
 * context is set up in the {@link Imported} using the {@link #add(Class...)}
 * and {@link #add(Imports)} methods.
 * </p>
 * <p>
 * The {@link #base()} {@link Imported} has most common JRE {@link Class}es
 * added. To not override a {@link Imported} context the {@link #fork()} method
 * is used to create a copy which then can be modified using
 * {@link #add(Class...)}.
 * </p>
 * <p>
 * This class is not thread-safe as it is not intended to be used in a
 * multi-threaded fashion. Instead threads should use the {@link #fork()} method
 * should they start from a shared {@link Imported} base.
 * </p>
 *
 * <h4>Example Inputs</h4>
 * <p>
 * All of the following input are valid and converted to the expected
 * {@link Type} value.
 * </p>
 *
 * <pre>
 * String
 * List&lt;String&gt;
 * List&lt;?&gt;
 * List&lt;? extends Number&gt;
 * Function&lt;?, List&lt;?&gt;&gt;
 * </pre>
 *
 * @author Jan Bernitt
 * @since 8.1
 */
public final class Imported {

	private static final Imported BASE = empty().add(String.class,
			CharSequence.class, StringBuilder.class, //
			Number.class, //
			char.class, Character.class, //
			byte.class, Byte.class, //
			short.class, Short.class, //
			int.class, Integer.class, //
			long.class, Long.class, //
			float.class, Float.class, //
			double.class, Double.class, //
			boolean.class, Boolean.class, //
			void.class, Void.class, //
			BigInteger.class, BigDecimal.class, //
			// common interfaces...
			Serializable.class, Comparable.class, Cloneable.class,
			Closeable.class,
			// common collection interface types...
			Collection.class, Iterable.class, Stream.class, //
			List.class, Set.class, //
			Map.class, ConcurrentMap.class,
			// common collection implementation types...
			ArrayList.class, LinkedList.class, //
			HashSet.class, TreeSet.class, //
			HashMap.class, TreeMap.class, ConcurrentHashMap.class, //
			// common functional interface types...
			Function.class, BiFunction.class, //
			LongFunction.class, IntFunction.class, DoubleFunction.class, //
			Supplier.class, Predicate.class, //
			Consumer.class, BiConsumer.class, //
			LongSupplier.class, IntSupplier.class, //
			BooleanSupplier.class, DoubleSupplier.class, //
			LongConsumer.class, IntConsumer.class, DoubleConsumer.class, //
			UnaryOperator.class, BinaryOperator.class, //
			// exceptions
			Exception.class, RuntimeException.class, Error.class//
	);

	private final Map<String, Type<?>> baseTypeBySimpleName;

	private Imported(Map<String, Type<?>> baseTypeBySimpleName) {
		this.baseTypeBySimpleName = baseTypeBySimpleName;
	}

	/**
	 * @return A new, completely empty {@link Imported} context.
	 */
	public static Imported empty() {
		return new Imported(new HashMap<>());
	}

	/**
	 * @return A {@link #fork()} of the standard {@link Imported} context
	 *         containing common JRE type 'imports'.
	 */
	public static Imported base() {
		return BASE.fork();
	}

	public Imported add(Imports src) {
		if (src != null)
			add(src.value());
		return this;
	}

	/**
	 * Adds all provided types to this {@link Imported} context.
	 *
	 * @param types list of types to add to this context (mutable change)
	 * @return this context for chaining
	 */
	public Imported add(Class<?>... types) {
		for (Class<?> imported : types)
			baseTypeBySimpleName.put(imported.getSimpleName(),
					Type.classType(imported));
		return this;
	}

	/**
	 * @return A new, forked instance of this {@link Imported} context that will
	 *         not be affected by further changes to its origin context.
	 */
	public Imported fork() {
		return new Imported(new HashMap<>(baseTypeBySimpleName));
	}

	/* Parsing full generic type signatures below... */

	public Type<?> resolve(String simpleTypeSignature) {
		return resolveUnknownType(simpleTypeSignature.trim());
	}

	private Type<?> resolveUnknownType(String signature) {
		if (signature.equals("?")) {
			return Type.WILDCARD;
		}
		if (signature.startsWith("? extends ")) {
			return resolve(signature.substring(10)).asUpperBound();
		}
		int arrayDimensions = 0;
		while (signature.endsWith("[]")) {
			arrayDimensions++;
			signature = signature.substring(0, signature.length() - 2);
		}
		Type<?> res = resolveGenericNonArrayType(signature);
		for (int i = 0; i < arrayDimensions; i++)
			res = res.addArrayDimension();
		return res;
	}

	private Type<?> resolveGenericNonArrayType(String signature) {
		if (!signature.endsWith(">")) {
			return resolveNonGenericNonArrayType(signature);
		}
		int start = signature.indexOf('<');
		String rawTypeName = signature.substring(0, start);
		Type<?> base = resolveNonGenericNonArrayType(rawTypeName);
		int expectedTypeParameters = base.rawType.getTypeParameters().length;
		Type<?>[] actualTypeParameters = new Type[expectedTypeParameters];
		for (int i = 0; i < expectedTypeParameters; i++) {
			int end = endOfTypeArgument(signature, start + 1);
			if (end < 0)
				throw new IllegalArgumentException("Expected a " + (i + 1)
					+ ". type parameter for type " + rawTypeName
					+ " but there seems to be non in " + signature);
			actualTypeParameters[i] = resolve(
					signature.substring(start + 1, end));
			start = end;
			if (i == expectedTypeParameters - 1
				&& signature.charAt(end) != '>') {
				illegalAt("Expected end of generic type arguments", signature,
						end);
			}
		}
		return base.parameterized(actualTypeParameters);
	}

	private static void illegalAt(String msg, String signature, int index) {
		char[] pad = new char[index];
		Arrays.fill(pad, ' ');
		throw new IllegalArgumentException(
				msg + ":\n" + signature + "\n" + String.valueOf(pad) + "^ here");
	}

	private Type<?> resolveNonGenericNonArrayType(String signature) {
		Type<?> type = baseTypeBySimpleName.get(signature);
		if (type == null)
			throw new IllegalArgumentException(
					"Missing import for type: `" + signature + "`");
		return type;
	}

	private static int endOfTypeArgument(String signature, int start) {
		int level = 1;
		int end = start;
		if (end >= signature.length())
			illegalAt("Unexpected end of type arguments list", signature, end);
		char c = signature.charAt(end);
		do {
			if (c == '<')
				level++;
			if (c == '>' || level == 1 && c == ',')
				level--;
			if (level > 0) {
				if (end + 1 >= signature.length())
					illegalAt("Unexpected end of type arguments list",
							signature, end);
				c = signature.charAt(++end);
			}
		} while (level > 0);
		return c == '>' || c == ',' ? end : -1;
	}

	public boolean equalTo(Imported other) {
		return baseTypeBySimpleName.equals(other.baseTypeBySimpleName);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Imported && equalTo((Imported) obj);
	}

	@Override
	public int hashCode() {
		return baseTypeBySimpleName.hashCode();
	}

	@Override
	public String toString() {
		return baseTypeBySimpleName.toString();
	}
}
