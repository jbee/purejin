package se.jbee.inject.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static se.jbee.inject.Type.raw;

import java.math.BigInteger;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

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

	private static class TestChainModule extends BinderModule {

		@Override
		protected void declare() {
			//TODO use more elegant way to declare...
			Converter<String, Long> str2int = Long::parseLong;
			bind(raw(Converter.class) //
					.parametized(String.class, Long.class)).to(str2int);
			Converter<Long, Integer> long2int = Long::intValue;
			bind(raw(Converter.class) //
					.parametized(Long.class, Integer.class)).to(long2int);
		}
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
