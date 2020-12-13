package se.jbee.inject.config;

import static java.util.Arrays.asList;
import static se.jbee.inject.lang.Type.fieldType;
import static se.jbee.inject.lang.Type.raw;
import static se.jbee.inject.lang.Utils.arrayFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import se.jbee.inject.Packages;
import se.jbee.inject.lang.Type;

/**
 * Picks the {@link Field}s that are bound as constants for the {@link Field}'s
 * {@link Type}.
 *
 * This is mainly used to allow declaring the bound {@link Type} simply by
 * making a field of that type. This way the {@link Type} is automatically
 * constructed and the user can use the normal Java language to define the
 * field. Also this makes sure the constant of the field value is assignable to
 * the bound type.
 *
 * @since 8.1
 */
@FunctionalInterface
public interface SharesBy {

	Field[] __noFields = new Field[0];

	/**
	 * @return The {@link Field}s that should be used in the context this
	 *         {@link SharesBy} is used.
	 */
	Field[] reflect(Class<?> impl);

	SharesBy noFields = impl -> __noFields;
	SharesBy declaredFields = ((SharesBy) Class::getDeclaredFields).ignoreSynthetic();
	SharesBy allFields = ((SharesBy) SharesBy::allFields).ignoreSynthetic();

	default SharesBy ignoreStatic() {
		return withModifier(((IntPredicate) Modifier::isStatic).negate());
	}

	default SharesBy ignoreSynthetic() {
		return select(field -> !field.isSynthetic());
	}

	default SharesBy ignoreGenericType() {
		return select(
				field -> !(field.getGenericType() instanceof TypeVariable<?>));
	}

	default SharesBy ignore(Predicate<Field> filter) {
		return select(filter.negate());
	}

	default SharesBy select(Predicate<Field> filter) {
		return impl -> arrayFilter(this.reflect(impl), filter);
	}

	default SharesBy typeAssignableTo(Type<?> supertype) {
		return select(field -> fieldType(field).isAssignableTo(supertype));
	}

	default SharesBy withModifier(IntPredicate filter) {
		return select(field -> filter.test(field.getModifiers()));
	}

	default SharesBy annotatedWith(Class<? extends Annotation> marker) {
		return select(field -> field.isAnnotationPresent(marker));
	}

	default SharesBy returnTypeIn(Packages filter) {
		return select(field -> filter.contains(raw(field.getType())));
	}

	default SharesBy in(Packages filter) {
		return impl -> filter.contains(raw(impl))
			? this.reflect(impl)
			: __noFields;
	}

	static Field[] allFields(Class<?> type) {
		List<Field> all = new ArrayList<>();
		while (type != Object.class && type != null) {
			all.addAll(asList(type.getDeclaredFields()));
			type = type.getSuperclass();
		}
		return all.toArray(__noFields);
	}
}
