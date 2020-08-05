package test.integration.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.jbee.inject.Dependency.dependency;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import se.jbee.inject.Provider;
import se.jbee.inject.Scope;
import se.jbee.inject.Scope.Controller;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.UnresolvableDependency.SupplyFailed;
import se.jbee.inject.scope.WorkerScope;

/**
 * Tests the correctness of the {@link WorkerScope}.
 */
public class TestWorkerScope {

	private final int generators = 3;
	private final Scope scope = new WorkerScope();

	@Test
	public void scopeYieldsControllerWithoutUsingTheProvider() {
		assertNotNull(getController());
	}

	@Test
	public void scopeCannotBeUsedBeforeItIsAllocated() {
		assertDeallocated(1, "test");
	}

	@Test
	public void deallocateDoesNotRequireAllocate() {
		getController().deallocate();
	}

	@Test
	public void deallocateCanBeCalledMoreThanOnce() {
		Scope.Controller controller = getController();
		controller.allocate();
		controller.deallocate();
		controller.deallocate();
	}

	@Test
	public void onceProvidedInstancesAreCachedInScope() {
		Scope.Controller controller = getController();
		controller.allocate();
		String v1 = "test";
		assertSame(v1, yieldProvided(1, v1));
		assertSame(v1, yieldExisting(1, String.class));
		controller.deallocate();
	}

	@Test
	public void onceDeallocatedInstancesAreNoLongerReachable() {
		Scope.Controller controller = getController();
		controller.allocate();
		String v1 = "test";
		assertSame(v1, yieldProvided(1, v1));
		controller.deallocate();
		assertDeallocated(1, String.class);
	}

	@Test
	public void contextCanBeTransferred() throws Exception {
		CompletableFuture<String> asyncResult = new CompletableFuture<>();
		Scope.Controller controllerLevel0 = getController();
		controllerLevel0.allocate();
		yieldProvided(1, "outer");
		Runnable asyncLevel1 = () -> {
			controllerLevel0.allocate(); // transfer level 0 => 1
			assertEquals("outer", yieldExisting(1, String.class));

			// somewhere else where no access to level 0 controller is given
			Controller controllerLevel1 = getController();
			Runnable asyncLevel2 = () -> {
				controllerLevel1.allocate(); // transfer level 1 => 2
				String actual = yieldExisting(1, String.class);
				assertEquals("outer", actual);
				controllerLevel1.deallocate();
				assertDeallocated(1, String.class);
				asyncResult.complete(actual);
			};
			new Thread(asyncLevel2).start();

			// back where allocate was done...
			controllerLevel0.deallocate();
			assertDeallocated(1, String.class);
		};
		Thread t1 = new Thread(asyncLevel1);
		t1.start();
		t1.join();
		assertEquals("outer", yieldExisting(1, String.class));
		// this also tests (sometimes) that level 2 can continue to run after level 1 has died
		assertEquals("outer", asyncResult.get());
	}

	private Scope.Controller getController() {
		return yieldExisting(0, Scope.Controller.class);
	}

	private <T> T yieldExisting(int serialID, Class<T> type) {
		return scope.provide(serialID, generators, dependency(type), () -> {
			throw new IllegalAccessError();
		});
	}

	private <T> T yieldProvided(int serialID, T constant) {
		@SuppressWarnings("unchecked")
		Class<T> type = (Class<T>) constant.getClass();
		AtomicBoolean provided = new AtomicBoolean();
		Provider<T> provider = () -> {
			provided.set(true);
			return constant;
		};
		T res = scope.provide(serialID, generators, dependency(type), provider);
		assertTrue("Provider was not called", provided.get());
		return res;
	}

	private void assertDeallocated(int serialID, Object typeOrConstant) {
		try {
			if (typeOrConstant.getClass() == Class.class) {
				yieldExisting(serialID, (Class<?>) typeOrConstant);
			} else {
				yieldProvided(serialID, typeOrConstant);
			}
			fail("Expected " + SupplyFailed.class.getSimpleName());
		} catch (UnresolvableDependency.SupplyFailed e) {
			assertEquals("Scope error", e.getMessage());
		}
	}

}
