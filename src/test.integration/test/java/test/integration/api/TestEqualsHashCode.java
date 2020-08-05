package test.integration.api;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPublic;
import static org.junit.Assert.*;
import static se.jbee.inject.lang.Utils.newArray;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.Test;
import se.jbee.inject.*;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Utils;

/**
 * Check the basic {@link #equals(Object)} and {@link #hashCode()}
 * implementations of value types.
 *
 * @author jan
 */
public class TestEqualsHashCode {

	@Test
	public void type() throws Exception {
		assertValidEqualsAndHashCodeContract(Type.class);
	}

	@Test
	public void instance() throws Exception {
		assertValidEqualsAndHashCodeContract(Instance.class);
	}

	@Test
	public void instances() throws Exception {
		assertValidEqualsAndHashCodeContract(Instances.class);
	}

	@Test
	public void locator() throws Exception {
		assertValidEqualsAndHashCodeContract(Locator.class);
	}

	@Test
	public void target() throws Exception {
		assertValidEqualsAndHashCodeContract(Target.class);
	}

	@Test
	public void packages() throws Exception {
		assertValidEqualsAndHashCodeContract(Packages.class);
	}

	@Test
	public void name() throws Exception {
		assertValidEqualsAndHashCodeContract(Name.class);
	}

	@Test
	public void dependency() throws Exception {
		assertValidEqualsAndHashCodeContract(Dependency.class);
	}

	static int base = 42;

	private static void assertValidEqualsAndHashCodeContract(Class<?> cls)
			throws Exception {
		Method equals = cls.getMethod("equals", Object.class);
		assertEquals(cls.getSimpleName() + " does not implement equals", cls,
				equals.getDeclaringClass());
		Method hashCode = cls.getMethod("hashCode");
		assertEquals(cls.getSimpleName() + " does not implement hashCode", cls,
				hashCode.getDeclaringClass());

		int baseA = base++;
		Object a = newInstance(cls, baseA);
		Object b = newInstance(cls, base++);
		Object a2 = newInstance(cls, baseA);
		assertEquals(a, a);
		assertEquals(a, a2);
		assertEquals(a2, a);
		assertEquals(a.hashCode(), a.hashCode());
		assertEquals(a.hashCode(), a2.hashCode());
		assertNotSame(a, b);
		assertNotSame(a, a2);
		assertNotEquals(a, b);
		assertNotEquals(b, a);
		assertNotEquals(a, null);
		assertNotEquals(b, null);

		assertTrue("Value type is not final.", isFinal(cls.getModifiers()));
		for (Field f : cls.getDeclaredFields()) {
			if (!f.isSynthetic()) {
				assertTrue("Field " + f.getName() + " is not final.",
						isFinal(f.getModifiers()));
			}
		}
		// while we are at it - cover toString as well
		assertFalse(a.toString().isEmpty());
		assertEquals(a.toString(), a2.toString());
		assertFalse(a.toString().equals(b.toString()));
	}

	private static Object newInstance(Class<?> cls, int base) throws Exception {
		if (cls == int.class || cls == Integer.class)
			return Integer.valueOf(base);
		if (cls == boolean.class || cls == Boolean.class)
			return Boolean.valueOf(base % 2 == 1);
		if (cls == String.class || cls == Object.class)
			return String.valueOf(base);
		if (cls == Class.class)
			return cls;
		if (cls == Type.class)
			return Type.raw(cls).upperBound(base % 2 == 1);
		if (cls == Instances.class)
			return Instances.ANY.push(
					(Instance<?>) newInstance(Instance.class, base+1));
		if (cls == Target.class)
			return Target.targeting(
					(Instance<?>) newInstance(Instance.class, base+1));
		if (cls == Dependency.class)
			return Dependency.dependency((Instance<?>) newInstance(Instance.class, base+1));
		if (cls.isArray()) {
			Object[] res = newArray(cls.getComponentType(), 1);
			res[0] = newInstance(cls.getComponentType(), base+1);
			return res;
		}
		Constructor<?> c = null;
		for (Constructor<?> ci : cls.getDeclaredConstructors()) {
			if (isPublic(ci.getModifiers())
					&& (c == null || ci.getParameterCount() > c.getParameterCount()))
				c = ci;
		}
		if (c == null) { // try to find a static factory method
			Method f = null;
			int n = base % 2;
			for (Method m : cls.getDeclaredMethods()) {
				if (isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers())
						&& (f == null
						|| m.getParameterCount() > f.getParameterCount()
						|| m.getParameterCount() == f.getParameterCount() && n == 1))
					f = m;
			}
			if (f != null) {
				return f.invoke(null, createArguments(cls, base+1, f.getParameterTypes()));
			}
		}
		if (c == null)
			return null;
		return c.newInstance(createArguments(cls, base+1, c.getParameterTypes()));
	}

	private static Object[] createArguments(Class<?> cls, int base,
			Class<?>[] types) throws Exception {
		Object[] args = new Object[types.length];
		for (int i = 0; i < args.length; i++) {
			if (types[i] != cls) {
				if (types[i].getComponentType() == cls) {
					args[i] = Utils.newArray(cls, 0);
				} else {
					args[i] = newInstance(types[i], base);
				}
			}
		}
		return args;
	}
}
