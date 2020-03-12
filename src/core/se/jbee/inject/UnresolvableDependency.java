/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A {@link Dependency} that should be resolved during the injection or
 * resolution process.
 * 
 * This is a grouping {@link RuntimeException} the problem specific exceptions
 * are derived from.
 * 
 * @see DependencyCycle
 * @see UnstableDependency
 * @see NoResourceForDependency
 * @see NoMethodForDependency
 * @see SupplyFailed
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class UnresolvableDependency extends RuntimeException {

	protected UnresolvableDependency(String message) {
		super(message);
	}

	protected UnresolvableDependency(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public String toString() {
		return getMessage();
	}

	/**
	 * A dependency cycle so that injection is not possible. Remove the cycle to
	 * resolve.
	 */
	public static final class DependencyCycle extends UnresolvableDependency {

		public DependencyCycle(Dependency<?> dependency,
				Locator<?> cycleTarget) {
			super("Cycle detected: " + dependency + " " + cycleTarget);
		}

	}

	/**
	 * It has been tried to inject a shorter living instance into one that will
	 * most likely outlive the injected one. This is considered to be
	 * unintentional. Use a indirection like a provider or services to resolve
	 * the problem.
	 */
	public static final class UnstableDependency
			extends UnresolvableDependency {

		public UnstableDependency(Injection parent, Injection injection) {
			super("Unstable dependency injection\n\tof: " + injection.target
				+ " " + injection.scoping + "\n\tinto: " + parent.target + " "
				+ parent.scoping);
		}

	}

	/**
	 * An {@link Injector} couldn't find a {@link Resource} that matches a
	 * {@link Dependency} to resolve.
	 */
	public static final class NoResourceForDependency
			extends UnresolvableDependency {

		public <T> NoResourceForDependency(Dependency<T> dependency,
				Resource<?>[] available, String msg) {
			super("No matching resource found.\n\t dependency: " + dependency
				+ "\n\tavailable are (for same raw type): "
				+ describe(available) + "\n\t" + msg);
		}

		public NoResourceForDependency(Collection<Type<?>> types,
				List<?> dropped) {
			super("No resource for type(s)\n\trequired: " + types
				+ "\n\tdrobbed bindings:\n" + dropped);
		}
	}

	public static String describe(Resource<?>... rs) {
		if (rs == null || rs.length == 0)
			return "none";
		StringBuilder b = new StringBuilder();
		for (Resource<?> rx : rs)
			b.append("\n    ").append(rx.locator.toString()).append(
					" defined ").append(rx.source);
		return b.toString();
	}

	/**
	 * A method has been described by its return and {@link Parameter}
	 * {@link Type}s (e.g. for use as factory or service) but such a method
	 * cannot be found. That usual means the defining class hasn't been bound
	 * correctly or the signature has changed.
	 */
	public static final class NoMethodForDependency
			extends UnresolvableDependency {

		public NoMethodForDependency(Type<?> returnType,
				Type<?>... parameterTypes) {
			this(returnType, parameterTypes, null);
		}

		public NoMethodForDependency(Type<?> returnType,
				Type<?>[] parameterTypes, Throwable cause) {
			super(returnType + ":" + Arrays.toString(parameterTypes), cause);
		}
	}

	/**
	 * When trying to supply a {@link Dependency} (e.g. by calling a method or
	 * constructor) a error occurred that prevented a successful supply.
	 */
	public static final class SupplyFailed extends UnresolvableDependency {

		public SupplyFailed(String msg, Throwable cause) {
			super(msg, cause);
		}

		public static SupplyFailed valueOf(Exception e,
				AccessibleObject invoked) {
			if (e instanceof InvocationTargetException) {
				Throwable t = ((InvocationTargetException) e).getTargetException();
				if (t instanceof Exception)
					e = (Exception) t;
			}
			return new SupplyFailed("Failed to invoke: " + invoked, e);
		}
	}

	public static final class IllegalAcccess extends UnresolvableDependency {

		public <T> IllegalAcccess(Locator<T> target,
				Dependency<? super T> dep) {
			super("Cannot access " + target + " directly for " + dep);
		}

	}

}