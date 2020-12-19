package se.jbee.inject.config;

import se.jbee.inject.Packages;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static se.jbee.inject.lang.Type.fieldType;
import static se.jbee.inject.lang.Type.raw;
import static se.jbee.inject.lang.Utils.arrayFilter;

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

	SharesBy OPTIMISTIC = declaredFields(f -> !f.isSynthetic(), true);

	/**
	 * @return The {@link Field}s that should be used in the context this {@link
	 * SharesBy} is used. Return {@code null} when no decision has been made.
	 * Returns empty array when the decision is to not share any fields.
	 */
	Field[] reflect(Class<?> impl);

	static SharesBy declaredFields(boolean includeInherited) {
		return declaredFields(null, includeInherited);
	}
	static SharesBy declaredFields(Predicate<Field> filter, boolean includeInherited) {
		return fields(Class::getDeclaredFields, filter, includeInherited);
	}

	static SharesBy fields(Function<Class<?>, Field[]> pool,
			Predicate<Field> filter, boolean includeInherited) {
		return fields(pool, filter,
				impl -> includeInherited ? Object.class : impl.getSuperclass());
	}

	static SharesBy fields(Function<Class<?>, Field[]> pool,
			Predicate<Field> filter, UnaryOperator<Class<?>> end) {
		return impl -> arrayFilter(impl, end.apply(impl), pool, filter)
				.toArray(new Field[0]);
	}

	default SharesBy orElse(SharesBy whenNull) {
		return impl -> {
			Field[] res = reflect(impl);
			return res != null ? res : whenNull.reflect(impl);
		};
	}

	default SharesBy or(SharesBy other) {
		return impl -> Utils.arrayConcat(reflect(impl), other.reflect(impl));
	}

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
			: null;
	}
}
