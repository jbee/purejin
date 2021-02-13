package test.integration.api;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Packages;
import se.jbee.lang.Type;

import java.text.Format;
import java.text.spi.DateFormatProvider;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Packages.packageOf;
import static se.jbee.lang.Type.raw;

class TestPackages {

	@Test
	void thatLowerBoundTypeIsNotInPackageJavaLang() {
		Packages javaLang = packageOf(String.class);
		assertFalse(javaLang.contains(Type.WILDCARD));
		assertFalse(javaLang.contains(raw(List.class).asUpperBound()));
	}

	@Test
	void thatPackageAllContainsAllTypes() {
		Packages all = Packages.ALL;
		assertTrue(all.contains(raw(List.class)));
		assertTrue(all.contains(raw(AtomicBoolean.class)));
		assertTrue(all.contains(raw(List.class).asUpperBound()));
	}

	@Test
	void thatPackageContainsType() {
		Packages javaUtil = Packages.packageOf(String.class);
		assertTrue(javaUtil.contains(raw(String.class)));
		assertFalse(javaUtil.contains(raw(ConcurrentLinkedQueue.class)));
		assertFalse(javaUtil.contains(raw(AtomicBoolean.class)));
		assertFalse(javaUtil.contains(raw(Format.class)));
	}

	@Test
	void thatSubpackagesContainType() {
		Packages javaUtil = Packages.subPackagesOf(List.class);
		assertFalse(javaUtil.contains(raw(List.class)));
		assertTrue(javaUtil.contains(raw(ConcurrentLinkedQueue.class)));
		assertTrue(javaUtil.contains(raw(AtomicBoolean.class)));
		assertFalse(javaUtil.contains(raw(Format.class)));
	}

	@Test
	void thatPackgeAndSubpackagesContainType() {
		Packages javaUtil = Packages.packageAndSubPackagesOf(List.class);
		assertTrue(javaUtil.contains(raw(List.class)));
		assertTrue(javaUtil.contains(raw(ConcurrentLinkedQueue.class)));
		assertTrue(javaUtil.contains(raw(AtomicBoolean.class)));
		assertFalse(javaUtil.contains(raw(Format.class)));
	}

	@Test
	void thatIndividualPackagesCanBeCherryPicked() {
		Packages cherries = Packages.packageOf(List.class, AtomicBoolean.class,
				String.class);
		assertTrue(cherries.contains(raw(List.class)));
		assertTrue(cherries.contains(raw(AtomicBoolean.class)));
		assertTrue(cherries.contains(raw(String.class)));
		assertTrue(cherries.contains(raw(Long.class)));
		assertFalse(cherries.contains(raw(Format.class)));
	}

	@Test
	void thatMultipleRootSubpackagesOfSameDepthCanBeCombined() {
		Packages subs = Packages.subPackagesOf(List.class, Format.class);
		assertFalse(subs.contains(raw(List.class))); // in java.util
		assertTrue(subs.contains(raw(AtomicBoolean.class))); // in java.uitl.concurrent
		assertFalse(subs.contains(raw(Format.class))); // in java.text
		assertTrue(subs.contains(raw(DateFormatProvider.class))); // in java.text.spi
	}

	@Test
	void thatMultipleRootPackagesAndSubpackagesOfSameDepthCanBeCombined() {
		Packages subs = Packages.packageAndSubPackagesOf(List.class,
				Format.class);
		assertTrue(subs.contains(raw(List.class))); // in java.util
		assertTrue(subs.contains(raw(AtomicBoolean.class))); // in java.uitl.concurrent
		assertTrue(subs.contains(raw(Format.class))); // in java.text
		assertTrue(subs.contains(raw(DateFormatProvider.class))); // in java.text.spi
	}

	@Test
	void thatMultipleRootSubpackagesOfDifferentDepthCanNotBeCombined() {
		assertThrows(IllegalArgumentException.class,
				() -> Packages.subPackagesOf(List.class, DateFormatProvider.class));
	}

	@Test
	void thatMultipleRootPackagesAndSubpackagesOfDifferentDepthCanNotBeCombined() {
		assertThrows(IllegalArgumentException.class,
				() -> Packages.packageAndSubPackagesOf(List.class, DateFormatProvider.class));
	}

	@Test
	void thatParentPackagesAreOfSameKindOfSet() {
		assertEquals(Packages.subPackagesOf(Map.class),
				Packages.subPackagesOf(ConcurrentMap.class).parents());
		assertEquals(Packages.packageOf(Map.class),
				Packages.packageOf(ConcurrentMap.class).parents());
		assertEquals(Packages.packageAndSubPackagesOf(Map.class),
				Packages.packageAndSubPackagesOf(
						ConcurrentMap.class).parents());
	}

	@Test
	void thatParentOfAllPackagesIsAllPackages() {
		assertEquals(Packages.ALL, Packages.ALL.parents());
	}

	@Test
	void thatParentOfDefaultPackageIsDefaultPackage() {
		assertEquals(Packages.DEFAULT, Packages.DEFAULT.parents());
	}
}
