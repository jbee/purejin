package test.integration.api;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.lang.Type;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Make sure the value objects of the library are {@link Serializable}.
 */
class TestSerializable {

	@Test
	void typeIsSerializable() {
		assertSerializable(Type.raw(String.class));
		assertSerializable(Type.raw(List.class).parameterized(Float.class));
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

	private static void assertSerializable(Serializable obj) {
		try {
			byte[] binObj = serialize(obj);
			Serializable obj2 = deserialize(binObj);
			assertEquals(obj, obj2);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	private static byte[] serialize(Serializable obj) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(obj);
		oos.close();
		return baos.toByteArray();
	}

	private static Serializable deserialize(byte[] b)
			throws IOException, ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		ObjectInputStream ois = new ObjectInputStream(bais);
		return (Serializable) ois.readObject();
	}
}
