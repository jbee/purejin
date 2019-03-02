package se.jbee.inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
	public void resourceIsSerializable() {
		assertSerializable(Resource.resource(String.class));
	}

	@Test
	public void sourceIsSerializable() {
		assertSerializable(Source.source(String.class));
	}

	@Test
	public void scopingIsSerializable() {
		assertSerializable(Scoping.IGNORE);
		assertSerializable(Scoping.scopingOf(Scope.class));
	}

	@Test
	public void injectionIsSerializable() {
		assertSerializable(new Injection(Instance.anyOf(String.class),
				Resource.resource(String.class), Scoping.IGNORE));
	}

	@Test
	public void dependencyIsSerializable() {
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
