package test.integration.convert;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Converter;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.convert.ConvertTo;
import se.jbee.inject.convert.ConverterModule;
import se.jbee.inject.convert.Converts;
import se.jbee.inject.convert.Imports;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.jbee.lang.Type.raw;

class TestConvertTo {

	@Imports({ String.class, Integer.class })
	@Converts({ "String", "Long", "Integer" })
	public static final class ExampleConverter
			implements Converter<Integer, BigInteger> {

		@Override
		public BigInteger convert(Integer input) {
			return BigInteger.valueOf(input.longValue());
		}
	}

	public static class TestChainModule extends ConverterModule {

		public Converter<String, Long> str2long = Long::parseLong;
		public Converter<Long, Integer> long2int = Long::intValue;

	}

	private final Injector context = Bootstrap.injector(TestChainModule.class);

	@Test
	void chainCanBeStartedFromAnyLinksInput() {
		ConvertTo<BigInteger> toBigInteger = new ConvertTo<>(new ExampleConverter(), context);
		assertConverts(toBigInteger, String.class, "42", BigInteger.valueOf(42));
		assertConverts(toBigInteger, Integer.class, 42, BigInteger.valueOf(42));
		assertConverts(toBigInteger, Long.class, 42L, BigInteger.valueOf(42));
	}

	private static <A, B> void assertConverts(ConvertTo<B> convertTo, Class<A> type,
			A input, B expected) {
		Converter<A, B> a2b = convertTo.from(raw(type));
		assertNotNull(a2b);
		assertEquals(expected, a2b.convert(input));
	}
}
