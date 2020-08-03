package se.jbee.inject.bootstrap;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Cast.listTypeOf;

import java.util.ArrayList;
import java.util.HashSet;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

public class TestInitialiserGenericBinds {

	static class StringList extends ArrayList<String> {
	}

	static class IntegerList extends ArrayList<Integer> {

	}

	static class TestInitialiserGenericBindsModule extends BinderModule {

		@Override
		protected void declare() {
			initbind(listTypeOf(String.class)).to((target, context) -> {
				target.add("a");
				return target;
			});
			initbind(listTypeOf(Integer.class)).to((target, context) -> {
				target.add(1);
				return target;
			});
			initbind(StringList.class).to((target, context) -> {
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
	public void initialisersOnlyAffectExactTypeMatches() {
		assertEquals(new HashSet<>(asList("b", "a")),
				new HashSet<>(context.resolve(listTypeOf(String.class))));
		assertEquals(asList(1), context.resolve(listTypeOf(Integer.class)));
	}
}
