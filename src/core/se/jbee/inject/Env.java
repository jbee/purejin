package se.jbee.inject;

import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

import se.jbee.inject.bootstrap.InconsistentBinding;

@FunctionalInterface
public interface Env {

	<T> T property(Name name, Type<T> property, Package pkg)
			throws InconsistentDeclaration;

	default <T> T property(Class<T> property, Package pkg)
			throws InconsistentDeclaration {
		return property(Name.DEFAULT, raw(property), pkg);
	}

	default <T> T property(Type<T> property, Package pkg)
			throws InconsistentDeclaration {
		return property(Name.DEFAULT, property, pkg);
	}

	default <E extends Enum<E>> boolean toggled(Class<E> property, E feature,
			Package pkg) {
		try {
			if (feature == null) {
				return property(named("null"), raw(property), pkg) == null;
			}
			return property(named(feature.name()),
					raw(feature.getDeclaringClass()), pkg) != null;
		} catch (InconsistentBinding e) {
			return false;
		}
	}

}
