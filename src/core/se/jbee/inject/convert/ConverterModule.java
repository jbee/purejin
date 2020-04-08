package se.jbee.inject.convert;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import se.jbee.inject.InconsistentDeclaration;
import se.jbee.inject.Provider;
import se.jbee.inject.Type;
import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.container.Supplier;

public abstract class ConverterModule extends BinderModule {

	public void autobindConvertersIn(Object obj) {
		for (Field field : obj.getClass().getDeclaredFields()) {
			autobindConverter(Type.fieldType(field), field, obj);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void autobindConverter(Type<T> type, Field field, Object obj) {
		autobindConverter(type, () -> (T) fieldValue(field, obj));
	}

	private <T> void autobindConverter(Type<T> valueType,
			Provider<T> valueProvider) {
		if (valueType.rawType == Converter.class) {
			Type<Converter<?, ?>> converterType = (Type<Converter<?, ?>>) valueType;
			bind(converterType).to((Converter<?, ?>) valueProvider.provide());

		} else if (valueType.rawType == Supplier.class
			&& valueType.parameter(0).rawType == Converter.class) {
			Type<Supplier<Converter<?, ?>>> supplierType = (Type<Supplier<Converter<?, ?>>>) valueType;
			Type<Converter<?, ?>> converterType = (Type<Converter<?, ?>>) supplierType.parameter(
					0);
			bind(converterType).toSupplier(
					(Supplier<Converter<?, ?>>) valueProvider.provide());
		}
	}

	private static Object fieldValue(Field f, Object source) {
		f.setAccessible(true);
		try {
			if (Modifier.isStatic(f.getModifiers())) {
				return f.get(null);
			}
			return f.get(source);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new InconsistentDeclaration(e.getMessage());
		}
	}
}
