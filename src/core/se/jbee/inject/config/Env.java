package se.jbee.inject.config;

import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

import java.lang.annotation.Annotation;

import se.jbee.inject.Name;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.InconsistentBinding;
import se.jbee.inject.declare.Macro;
import se.jbee.inject.declare.ModuleWith;

@FunctionalInterface
public interface Env {

	<T> T property(Name name, Type<T> property, Package pkg);

	default <T> T property(Class<T> property, Package pkg) {
		return property(Name.DEFAULT, raw(property), pkg);
	}

	default <T> T property(Type<T> property, Package pkg) {
		return property(Name.DEFAULT, property, pkg);
	}

	@SuppressWarnings("unchecked")
	default <T> Macro<T> ifBound(Class<T> as, Package pkg) {
		return property(raw(Macro.class).parametized(Type.classType(as)), pkg);
	}

	@SuppressWarnings("unchecked")
	default ModuleWith<Class<?>> annotationProperty(
			Class<? extends Annotation> by, Package pkg) {
		return property(Name.named(by),
				raw(ModuleWith.class).parametized(Type.CLASS), pkg);
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
