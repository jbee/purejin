package test.integration.convert;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Converter;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.convert.ConverterModule;
import se.jbee.lang.Type;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.lang.Cast.listTypeOf;
import static se.jbee.lang.Type.raw;
import static se.jbee.lang.Utils.arrayMap;

class TestCollectionConverter {

	public static class TestCollectionConverterModule extends ConverterModule {

		public static final Converter<String, Integer> str2int = Integer::parseInt;
		public static final Converter<? extends Object[], List<?>> arr2list = Arrays::asList;

		public static <B> Converter<String, B[]> toArray(Type<B> elementType,
				Converter<String, B> elementConverter) {
			return in -> arrayMap(in.split(","), elementType.rawType,
					elementConverter::convert);
		}
	}

	private final Injector context = Bootstrap.injector(
			TestCollectionConverterModule.class);

	@Test
	void genericArrayToCollectionConverterCanBeDefined() {
		Converter<Number[], List<Number>> arr2list = context.resolve(
				Converter.converterTypeOf(raw(Number[].class), listTypeOf(Number.class)));
		assertEquals(asList(1, 2), arr2list.convert(new Number[] { 1, 2 }));
	}

	@Test
	void genericStringToArrayConverterCanBeDefined() {
		Converter<String, Integer[]> str2intArr = context.resolve(
				Converter.converterTypeOf(String.class, Integer[].class));
		assertArrayEquals(new Integer[] { 42, 13 },
				str2intArr.convert("42,13"));
	}

}
