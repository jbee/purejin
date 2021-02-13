package test.integration.convert;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Converter;
import se.jbee.inject.Injector;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.convert.ConverterModule;
import se.jbee.lang.Type;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Resource.resourceTypeOf;
import static se.jbee.lang.Cast.listTypeOf;
import static se.jbee.lang.Type.classType;
import static se.jbee.lang.Type.raw;
import static se.jbee.lang.Utils.arrayMap;

/**
 * Tests the basics of {@link Converter}s.
 */
class TestConverter {

	public static class TestConverterModule extends ConverterModule {

		public Converter<String, Integer> str2int = Integer::parseInt;
		public Converter<String, Long> str2long = Long::parseLong;

		public <B> Converter<String, B[]> toArray(Type<B> elementType,
				Converter<String, B> element) {
			return input -> arrayMap(input.split("\\s*,\\s*"),
					elementType.rawType, element::convert);
		}

		public <B> Converter<String, List<B>> toList(Converter<String, B[]> array) {
			return input -> asList(array.convert(input));
		}
	}

	@Test
	void chainsCanBeBuildByAppending() {
		Converter<String, Long> str2long = Long::parseLong;
		Converter<Long, Integer> long2int = Long::intValue;
		assertEquals(Integer.valueOf(13),
				str2long.then(long2int).convert("13"));
	}

	@Test
	void chainsCanBeBuildByPrepending() {
		Converter<String, Long> str2long = Long::parseLong;
		Converter<Long, Integer> long2int = Long::intValue;
		assertEquals(Integer.valueOf(13),
				long2int.upon(str2long).convert("13"));
	}

	@Test
	void errorsCanBeRecoveredUsingDefaultValues() {
		Converter<String, Integer> str2int = Integer::parseInt;
		assertEquals(13, str2int.orElse(13).convert("illegal").intValue());
	}

	private final Injector context = Bootstrap.injector(
			TestConverterModule.class);

	@Test
	void converterMethodsWithTypeVariableUseScopeDependencyType() {
		@SuppressWarnings("rawtypes")
		Resource<Converter<String, List>> str2ints = context.resolve(
				resourceTypeOf(Converter.converterTypeOf(raw(String.class),
						classType(List.class))));
		assertEquals(Scope.dependencyType, str2ints.lifeCycle.scope);
	}

	@Test
	void genericMethodConverters() {
		Converter<String, List<Integer>> str2ints = context.resolve(
				Converter.converterTypeOf(raw(String.class), listTypeOf(Integer.class)));
		assertEquals(asList(42, 13), str2ints.convert("42, 13"));
		Converter<String, List<Long>> str2longs = context.resolve(
				Converter.converterTypeOf(raw(String.class), listTypeOf(Long.class)));
		assertEquals(asList(42L, 13L), str2longs.convert("42, 13"));
	}
}
