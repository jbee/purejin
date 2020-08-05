package test.integration.convert;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Cast.listTypeOf;
import static se.jbee.inject.lang.Type.raw;
import static se.jbee.inject.lang.Utils.arrayMap;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import se.jbee.inject.Converter;
import se.jbee.inject.Injector;
import se.jbee.inject.lang.Type;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.convert.ConverterModule;

public class TestCollectionConverter {

	static class TestCollectionConverterModule extends ConverterModule {

		static final Converter<String, Integer> str2int = Integer::parseInt;
		static final Converter<? extends Object[], List<?>> arr2list = Arrays::asList;

		static <B> Converter<String, B[]> toArray(Type<B> elementType,
				Converter<String, B> elementConverter) {
			return in -> arrayMap(in.split(","), elementType.rawType,
					elementConverter::convert);
		}
	}

	private final Injector context = Bootstrap.injector(
			TestCollectionConverterModule.class);

	@Test
	public void genericArrayToCollectionConverterCanBeDefined() {
		Converter<Number[], List<Number>> arr2list = context.resolve(
				Converter.converterTypeOf(raw(Number[].class), listTypeOf(Number.class)));
		assertEquals(asList(1, 2), arr2list.convert(new Number[] { 1, 2 }));
	}

	@Test
	public void genericStringToArrayConverterCanBeDefined() {
		Converter<String, Integer[]> str2intArr = context.resolve(
				Converter.converterTypeOf(String.class, Integer[].class));
		assertArrayEquals(new Integer[] { 42, 13 },
				str2intArr.convert("42,13"));
	}

}
