package test.integration.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static se.jbee.inject.lang.Type.raw;

import java.math.BigInteger;

import org.junit.Test;

import se.jbee.inject.Converter;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.convert.Chain;
import se.jbee.inject.convert.ConverterModule;
import se.jbee.inject.convert.Converts;
import se.jbee.inject.convert.Imports;

public class TestChain {

	@Imports({ String.class, Integer.class })
	@Converts({ "String", "Long", "Integer" })
	static final class ExampleConverter
			implements Converter<Integer, BigInteger> {

		@Override
		public BigInteger convert(Integer input) {
			return BigInteger.valueOf(input.longValue());
		}
	}

	static class TestChainModule extends ConverterModule {

		Converter<String, Long> str2long = Long::parseLong;
		Converter<Long, Integer> long2int = Long::intValue;

	}

	private final Injector context = Bootstrap.injector(TestChainModule.class);

	@Test
	public void chainCanBeStartedFromAnyLinksInput() {
		Chain<BigInteger> chain = new Chain<>(new ExampleConverter(), context);
		assertConverts(chain, String.class, "42", BigInteger.valueOf(42));
		assertConverts(chain, Integer.class, 42, BigInteger.valueOf(42));
		assertConverts(chain, Long.class, 42L, BigInteger.valueOf(42));
	}

	private static <I, O> void assertConverts(Chain<O> chain, Class<I> type,
			I input, O expected) {
		Converter<I, O> str2bigInt = chain.forInput(raw(type));
		assertNotNull(str2bigInt);
		assertEquals(expected, str2bigInt.convert(input));
	}
}
