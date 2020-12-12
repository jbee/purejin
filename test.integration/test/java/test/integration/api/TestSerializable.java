package test.integration.api;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.lang.Type;

import java.io.Serializable;
import java.util.List;

import static test.integration.util.TestUtils.assertSerializable;

/**
 * Make sure the value objects of the library are {@link Serializable}.
 */
class TestSerializable {

	@Test
	void typeIsSerializable() {
		assertSerializable(Type.raw(String.class));
		assertSerializable(Type.raw(List.class).parametized(Float.class));
	}

	@Test
	void nameIsSerializable() {
		assertSerializable(Name.ANY);
		assertSerializable(Name.DEFAULT);
		assertSerializable(Name.named("foo"));
	}

	@Test
	void instanceIsSerializable() {
		assertSerializable(Instance.ANY);
		assertSerializable(
				Instance.instance(Name.named("bar"), Type.raw(String.class)));
	}

	@Test
	void InstancesIsSerializable() {
		assertSerializable(Instances.ANY);
		assertSerializable(Instances.ANY.push(Instance.anyOf(String.class)));
	}

	@Test
	void packagesIsSerializable() {
		assertSerializable(Packages.ALL);
		assertSerializable(Packages.DEFAULT);
		assertSerializable(Packages.packageAndSubPackagesOf(Type.class));
	}

	@Test
	void targetIsSerializable() {
		assertSerializable(Target.ANY);
		assertSerializable(Target.targeting(String.class));
	}

	@Test
	void locatorIsSerializable() {
		assertSerializable(Locator.locator(String.class));
	}

	@Test
	void sourceIsSerializable() {
		assertSerializable(Source.source(String.class));
	}

	@Test
	void scopePermanenceIsSerializable() {
		assertSerializable(ScopeLifeCycle.ignore);
		assertSerializable(ScopeLifeCycle.singleton.derive(Scope.application));
	}

	@Test
	void injectionIsSerializable() {
		assertSerializable(new Injection(Instance.anyOf(String.class),
				Locator.locator(String.class), ScopeLifeCycle.ignore));
	}

	@Test
	void dependencyIsSerializable() {
		Dependency<String> dep = Dependency.dependency(String.class);
		assertSerializable(dep);
		assertSerializable(dep.injectingInto(Integer.class));
	}

}
