package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import java.util.ArrayList;
import java.util.HashSet;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Cast.listTypeOf;

class TestBuildUpGenericBinds {

	public static class StringList extends ArrayList<String> {
	}

	public static class IntegerList extends ArrayList<Integer> {

	}

	static class TestInitialiserGenericBindsModule extends BinderModule {

		@Override
		protected void declare() {
			initbind(listTypeOf(String.class)).to((target, as, context) -> {
				target.add("a");
				return target;
			});
			initbind(listTypeOf(Integer.class)).to((target, as, context) -> {
				target.add(1);
				return target;
			});
			initbind(StringList.class).to((target, as, context) -> {
				target.add("b");
				return target;
			});
			bind(listTypeOf(String.class)).to(StringList.class);
			bind(listTypeOf(Integer.class)).to(IntegerList.class);
		}

	}

	private final Injector context = Bootstrap.injector(
			TestInitialiserGenericBindsModule.class);

	@Test
	void initialisersOnlyAffectExactTypeMatches() {
		assertEquals(new HashSet<>(asList("b", "a")),
				new HashSet<>(context.resolve(listTypeOf(String.class))));
		assertEquals(asList(1), context.resolve(listTypeOf(Integer.class)));
	}
}
