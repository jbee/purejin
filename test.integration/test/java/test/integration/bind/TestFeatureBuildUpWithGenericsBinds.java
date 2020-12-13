package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import java.util.ArrayList;
import java.util.HashSet;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.lang.Cast.listTypeOf;

class TestFeatureBuildUpWithGenericsBinds {

	public static class StringList extends ArrayList<String> {
	}

	public static class IntegerList extends ArrayList<Integer> {

	}

	static class TestFeatureBuildUpWithGenericsBindsModule extends BinderModule {

		@Override
		protected void declare() {
			upbind(listTypeOf(String.class)).to((target, as, context) -> {
				target.add("a");
				return target;
			});
			upbind(listTypeOf(Integer.class)).to((target, as, context) -> {
				target.add(1);
				return target;
			});
			upbind(StringList.class).to((target, as, context) -> {
				target.add("b");
				return target;
			});
			bind(listTypeOf(String.class)).to(StringList.class);
			bind(listTypeOf(Integer.class)).to(IntegerList.class);
		}

	}

	private final Injector context = Bootstrap.injector(
			TestFeatureBuildUpWithGenericsBindsModule.class);

	@Test
	void buildUpOnlyAffectExactTypeMatches() {
		assertEquals(new HashSet<>(asList("b", "a")),
				new HashSet<>(context.resolve(listTypeOf(String.class))));
		assertEquals(asList(1), context.resolve(listTypeOf(Integer.class)));
	}
}
