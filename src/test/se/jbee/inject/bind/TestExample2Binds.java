package se.jbee.inject.bind;

import static org.junit.Assert.assertNotSame;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * This illustrates how to use different named instances for the same interface
 * that are all implemented by the same class without having them linked to the
 * same instance.
 */
public class TestExample2Binds {

	interface Decoupling {
		
	}
	
	static class DefaultImpl implements Decoupling {
		
	}
	
	static class Example2Module extends BinderModule {

		@Override
		protected void declare() {
			bind(named("a"), Decoupling.class).toConstructor(DefaultImpl.class);
			bind(named("b"), Decoupling.class).toConstructor(DefaultImpl.class);
			bind(named("c"), Decoupling.class).toConstructor(DefaultImpl.class);
			bind(named("d"), Decoupling.class).toConstructor(DefaultImpl.class);
		}
		
	}
	
	@Test
	public void thatInstancesAToDAreDifferent() {
		Injector injector = Bootstrap.injector(Example2Module.class);
		Decoupling a = injector.resolve(dependency(instance(named("a"), raw(Decoupling.class))));
		Decoupling b = injector.resolve(dependency(instance(named("b"), raw(Decoupling.class))));
		Decoupling c = injector.resolve(dependency(instance(named("c"), raw(Decoupling.class))));
		Decoupling d = injector.resolve(dependency(instance(named("d"), raw(Decoupling.class))));
		
		assertNotSame(a, b);
		assertNotSame(a, c);
		assertNotSame(a, d);
		assertNotSame(b, c);
		assertNotSame(b, d);
		assertNotSame(c, d);
	}
}
