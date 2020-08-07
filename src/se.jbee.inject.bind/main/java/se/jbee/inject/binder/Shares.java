package se.jbee.inject.binder;

import static java.lang.reflect.Modifier.isStatic;
import static se.jbee.inject.lang.Type.fieldType;

import java.lang.reflect.Field;

import se.jbee.inject.Supplier;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Typed;

/**
 * Shares a value that is extracted from a field. The field is read each time
 * the {@link Supplier} supplying is asked to supply the value.
 *
 * @author Jan Bernitt
 * @since 19.1
 *
 * @param <T> type of the constant shared
 */
public final class Shares<T> implements Typed<T> {

	public static <T> Shares<T> shares(Object owner, Field target) {
		return new Shares<>(owner, target);
	}

	public final Object owner;
	public final Field target;
	public final Type<T> type;
	public final boolean isInstanceField;

	@SuppressWarnings("unchecked")
	private Shares(Object owner, Field target) {
		this.isInstanceField = !isStatic(target.getModifiers());
		this.owner = isInstanceField ? owner : null;
		this.target = target;
		this.type = (Type<T>) fieldType(target);
	}

	@Override
	public Type<T> type() {
		return type;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> Shares<E> typed(Type<E> supertype) {
		type().castTo(supertype); // make sure is valid
		return (Shares<E>) this;
	}
}
