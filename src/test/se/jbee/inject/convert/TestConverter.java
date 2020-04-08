package se.jbee.inject.convert;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestConverter {

	@Test
	public void chainsCanBeBuildByAppending() {
		Converter<String, Long> str2long = Long::parseLong;
		Converter<Long, Integer> long2int = Long::intValue;
		assertEquals(Integer.valueOf(13),
				str2long.andThen(long2int).convert("13"));
	}

	@Test
	public void chainsCanBeBuildByPrepanding() {
		Converter<String, Long> str2long = Long::parseLong;
		Converter<Long, Integer> long2int = Long::intValue;
		assertEquals(Integer.valueOf(13),
				long2int.after(str2long).convert("13"));
	}

	@Test
	public void errorsCanBeRecoveredUsingDefaultValues() {
		Converter<String, Integer> str2int = Integer::parseInt;
		assertEquals(13, str2int.orOnError(13).convert("illegal").intValue());
	}
}
