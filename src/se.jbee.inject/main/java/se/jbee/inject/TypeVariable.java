package se.jbee.inject;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * A utility to help extract actual {@link Type} of
 * {@link java.lang.reflect.TypeVariable}s.
 * 
 * @author Jan Bernitt
 * @since 19.1
 */
public final class TypeVariable {

	/**
	 * Returns a map with type variable names as keys and a {@link Function} as
	 * value that given an actual {@link Type} of the declared
	 * {@link java.lang.reflect.Type} will extract the actual {@link Type} for
	 * the type variable.
	 * 
	 * @param type a generic type as returned by Java reflect for generic type
	 *            of for methods, fields or parameters
	 * @return
	 */
	public static Map<String, UnaryOperator<Type<?>>> typeVariables(
			java.lang.reflect.Type type) {
		if (type instanceof Class)
			return emptyMap();
		if (type instanceof GenericArrayType) {
			Map<String, UnaryOperator<Type<?>>> vars = new HashMap<>();
			typeVariables(
					((GenericArrayType) type).getGenericComponentType()).forEach(
							(k, v) -> vars.put(k, t -> v.apply(t).baseType()));
			return vars;
		}
		if (type instanceof WildcardType) {
			Map<String, UnaryOperator<Type<?>>> vars = new HashMap<>();
			for (java.lang.reflect.Type upperBound : ((WildcardType) type).getUpperBounds()) {
				vars.putAll(typeVariables(upperBound));
			}
			return vars;
		}
		if (type instanceof ParameterizedType) {
			Map<String, UnaryOperator<Type<?>>> vars = new HashMap<>();
			java.lang.reflect.Type[] generics = ((ParameterizedType) type).getActualTypeArguments();
			for (int i = 0; i < generics.length; i++) {
				java.lang.reflect.Type generic = generics[i];
				int index = i;
				typeVariables(generic).forEach((k, v) -> vars.put(k,
						t -> v.apply(t.parameter(index))));
			}
			return vars;
		}
		if (type instanceof java.lang.reflect.TypeVariable) {
			return singletonMap(
					((java.lang.reflect.TypeVariable<?>) type).getName(),
					UnaryOperator.identity());
		}
		throw new UnsupportedOperationException(
				"Type has no support yet: " + type);
	}
}
