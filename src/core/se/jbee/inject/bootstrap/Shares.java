package se.jbee.inject.bootstrap;

import static java.lang.reflect.Modifier.isStatic;
import static se.jbee.inject.Type.fieldType;
import static se.jbee.inject.Utils.accessible;

import java.lang.reflect.Field;

import se.jbee.inject.Type;
import se.jbee.inject.Typed;

/**
 * Shares a constant value that is extracted from a field.
 * 
 * @author Jan Bernitt
 * @since 19.1
 *
 * @param <T> type of the constant shared
 */
public final class Shares<T> implements Typed<T> {

	public static <T> Shares<T> shares(Object owner, Field constant) {
		return new Shares<>(owner, constant);
	}

	public final Object owner;
	public final Field constant;
	public final Type<T> type;
	public final boolean isInstanceField;

	@SuppressWarnings("unchecked")
	private Shares(Object owner, Field constant) {
		this.isInstanceField = !isStatic(constant.getModifiers());
		this.owner = isInstanceField ? owner : null;
		this.constant = accessible(constant);
		this.type = (Type<T>) fieldType(constant);
	}

	@Override
	public Type<T> type() {
		return type;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> Typed<E> typed(Type<E> supertype) {
		type().castTo(supertype); // make sure is valid
		return (Shares<E>) this;
	}
}
