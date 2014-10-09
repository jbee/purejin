package se.jbee.inject.bind;

import org.junit.Assert;
import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * @author martin.nycander
 */
public class TestDecoratorBinds {
    private final Injector injector = Bootstrap.injector(DecoratorModule.class);

    private static class DecoratorModule extends BinderModule {

        @Override
        protected void declare() {
            bind(Foo.class).to(FooDecorator.class);
            injectingInto(FooDecorator.class).bind(Foo.class).to(Bar.class);
        }
    }

    private static interface Foo {
    }

    private static class FooDecorator implements Foo {
        private final FooDecorator original;
        public FooDecorator(FooDecorator original) {
            this.original = original;
        }
    }

    private static class Bar implements Foo {
        public Bar() {
        }
    }

    @Test
    public void TestDecorator() {
        Assert.assertEquals(FooDecorator.class, injector.resolve(Dependency.dependency(Foo.class)).getClass());
    }
}
