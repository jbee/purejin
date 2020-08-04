package test.integration.convert;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Cast.listTypeOf;
import static se.jbee.inject.Cast.resourceTypeFor;
import static se.jbee.inject.Type.classType;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.arrayMap;

import java.util.List;

import org.junit.Test;

import se.jbee.inject.Converter;
import se.jbee.inject.Injector;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.convert.ConverterModule;

/**
 * Tests the basics of {@link Converter}s.
 */
public class TestConverter {

	static class TestConverterModule extends ConverterModule {

		Converter<String, Integer> str2int = Integer::parseInt;
		Converter<String, Long> str2long = Long::parseLong;

		<B> Converter<String, B[]> toArray(Type<B> elementType,
				Converter<String, B> element) {
			return input -> arrayMap(input.split("\\s*,\\s*"),
					elementType.rawType, element::convert);
		}

		<B> Converter<String, List<B>> toList(Converter<String, B[]> array) {
			return input -> asList(array.convert(input));
		}
	}

	@Test
	public void chainsCanBeBuildByAppending() {
		Converter<String, Long> str2long = Long::parseLong;
		Converter<Long, Integer> long2int = Long::intValue;
		assertEquals(Integer.valueOf(13),
				str2long.before(long2int).convert("13"));
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
		assertEquals(13, str2int.fallbackTo(13).convert("illegal").intValue());
	}

	private final Injector context = Bootstrap.injector(
			TestConverterModule.class);

	@Test
	public void converterMethodsWithTypeVariableUseScopeDependencyType() {
		@SuppressWarnings("rawtypes")
		Resource<Converter<String, List>> str2ints = context.resolve(
				resourceTypeFor(Converter.type(raw(String.class),
						classType(List.class))));
		assertEquals(Scope.dependencyType, str2ints.permanence.scope);
	}

	@Test
	public void genericMethodConverters() {
		Converter<String, List<Integer>> str2ints = context.resolve(
				Converter.type(raw(String.class), listTypeOf(Integer.class)));
		assertEquals(asList(42, 13), str2ints.convert("42, 13"));
		Converter<String, List<Long>> str2longs = context.resolve(
				Converter.type(raw(String.class), listTypeOf(Long.class)));
		assertEquals(asList(42L, 13L), str2longs.convert("42, 13"));
	}
}
