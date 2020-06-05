package se.jbee.inject;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPublic;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Utils.newArray;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Test;

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
		assertFalse(a.equals(b));
		assertFalse(b.equals(a));
		assertFalse(a.equals(null));
		assertFalse(b.equals(null));

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
		if (cls == String.class)
			return String.valueOf(base);
		if (cls == Class.class)
			return cls;
		if (cls.isArray()) {
			Object[] res = newArray(cls.getComponentType(), 1);
			res[0] = newInstance(cls.getComponentType(), base++);
			return res;
		}
		Constructor<?> c = null;
		for (Constructor<?> ci : cls.getDeclaredConstructors()) {
			if (c == null || ci.getParameterCount() > c.getParameterCount())
				c = ci;
		}
		if (c == null)
			return null;
		if (!isPublic(c.getModifiers())) {
			c.setAccessible(true);
		}
		Class<?>[] pTypes = c.getParameterTypes();
		Object[] args = new Object[pTypes.length];
		for (int i = 0; i < args.length; i++) {
			if (pTypes[i] != cls) {
				if (pTypes[i].getComponentType() == cls) {
					args[i] = Utils.newArray(cls, 0);
				} else {
					args[i] = newInstance(pTypes[i], base++);
				}
			}
		}
		return c.newInstance(args);
	}
}
