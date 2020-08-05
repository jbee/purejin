package se.jbee.inject;

import se.jbee.inject.lang.Type;

import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Type.raw;

@FunctionalInterface
public interface Env {

	<T> T property(Name name, Type<T> property, Package scope)
			throws InconsistentDeclaration;

	default <T> T property(Class<T> property, Package scope)
			throws InconsistentDeclaration {
		return property(Name.DEFAULT, raw(property), scope);
	}

	default <T> T property(Type<T> property, Package scope)
			throws InconsistentDeclaration {
		return property(Name.DEFAULT, property, scope);
	}

	default <T> T globalProperty(Name name, Type<T> property) {
		return property(name, property, null);
	}

	default <T> T globalProperty(Type<T> property) {
		return property(property, null);
	}

	default <T> T globalProperty(Class<T> property) {
		return property(property, null);
	}

	default <E extends Enum<E>> boolean toggled(Class<E> property, E feature,
			Package scope) {
		try {
			if (feature == null) {
				return property(named("null"), raw(property), scope) == null;
			}
			return property(named(feature.name()),
					raw(feature.getDeclaringClass()), scope) != null;
		} catch (InconsistentDeclaration e) {
			return false;
		}
	}

}
