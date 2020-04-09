package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Type.raw;

import java.util.function.Function;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.ProducesBy;

public class TestTypeVariableAutobindBinds {

	static class TestTypeVariableAutobindBindsModule extends BinderModule {

		@Override
		protected void declare() {
			autobind().produceBy(ProducesBy.declaredMethods).in(this);
		}

		<T> Function<T, String> actualTypeAndValue(
				Type<Function<T, String>> valueType) {
			return val -> valueType.toString() + ":" + val.toString();
		}
	}

	private final Injector context = Bootstrap.injector(
			TestTypeVariableAutobindBindsModule.class);

	@Test
	public void test() {
		Function<Integer, String> f = context.resolve(
				raw(Function.class).parametized(Integer.class, String.class));
		assertEquals(
				"java.util.function.Function<java.lang.Integer,java.lang.String>:42",
				f.apply(42));
	}
}
