package se.jbee.inject;

import static se.jbee.inject.util.TestUtils.assertSerializable;

import java.io.Serializable;
import java.util.List;

import org.junit.Test;

/**
 * Make sure the value objects of the library are {@link Serializable}.
 */
public class TestSerializable {

	@Test
	public void typeIsSerializable() {
		assertSerializable(Type.raw(String.class));
		assertSerializable(Type.raw(List.class).parametized(Float.class));
	}

	@Test
	public void nameIsSerializable() {
		assertSerializable(Name.ANY);
		assertSerializable(Name.DEFAULT);
		assertSerializable(Name.named("foo"));
	}

	@Test
	public void instanceIsSerializable() {
		assertSerializable(Instance.ANY);
		assertSerializable(
				Instance.instance(Name.named("bar"), Type.raw(String.class)));
	}

	@Test
	public void InstancesIsSerializable() {
		assertSerializable(Instances.ANY);
		assertSerializable(Instances.ANY.push(Instance.anyOf(String.class)));
	}

	@Test
	public void packagesIsSerializable() {
		assertSerializable(Packages.ALL);
		assertSerializable(Packages.DEFAULT);
		assertSerializable(Packages.packageAndSubPackagesOf(Type.class));
	}

	@Test
	public void targetIsSerializable() {
		assertSerializable(Target.ANY);
		assertSerializable(Target.targeting(String.class));
	}

	@Test
	public void locatorIsSerializable() {
		assertSerializable(Locator.locator(String.class));
	}

	@Test
	public void sourceIsSerializable() {
		assertSerializable(Source.source(String.class));
	}

	@Test
	public void scopingIsSerializable() {
		assertSerializable(ScopePermanence.ignore);
		assertSerializable(ScopePermanence.singleton.derive(Scope.application));
	}

	@Test
	public void injectionIsSerializable() {
		assertSerializable(new Injection(Instance.anyOf(String.class),
				Locator.locator(String.class), ScopePermanence.ignore));
	}

	@Test
	public void dependencyIsSerializable() {
		Dependency<String> dep = Dependency.dependency(String.class);
		assertSerializable(dep);
		assertSerializable(dep.injectingInto(Integer.class));
	}

}
