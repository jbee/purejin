/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Utils.arrayOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.jbee.inject.DeclarationType;
import se.jbee.inject.Env;
import se.jbee.inject.Locator;
import se.jbee.inject.Name;
import se.jbee.inject.Qualifying;
import se.jbee.inject.Source;
import se.jbee.inject.Type;
import se.jbee.inject.Typed;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.container.Injectee;
import se.jbee.inject.container.Supplier;
import se.jbee.inject.declare.Bundle;
import se.jbee.inject.declare.Module;

/**
 * A {@link Binding} is implements the {@link Injectee} created during the
 * bootstrapping process based on {@link Bindings}, {@link Bundle}s and
 * {@link Module}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 *
 * @param <T> The type of the bound value (instance)
 */
public final class Binding<T> extends Injectee<T>
		implements Comparable<Binding<?>>, Module, Typed<T> {

	public static <T> Binding<T> binding(Locator<T> locator, BindingType type,
			Supplier<? extends T> supplier, Name scope, Source source) {
		return new Binding<>(locator, type, supplier, scope, source);
	}

	public final BindingType type;

	private Binding(Locator<T> locator, BindingType type,
			Supplier<? extends T> supplier, Name scope, Source source) {
		super(scope, locator, supplier, source);
		this.type = type;
	}

	@Override
	public Type<T> type() {
		return locator.type();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> Binding<E> typed(Type<E> type) {
		return new Binding<>(locator.typed(type().toSupertype(type)), this.type,
				(Supplier<? extends E>) supplier, scope, source);
	}

	public boolean isComplete() {
		return supplier != null;
	}

	public Binding<T> complete(BindingType type,
			Supplier<? extends T> supplier) {
		if (type == BindingType.MACRO)
			throw InconsistentBinding.illegalCompletion(this, type);
		return new Binding<>(locator, type, supplier, scope, source);
	}

	@Override
	public void declare(Bindings bindings, Env env) {
		bindings.add(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Binding))
			return false;
		Binding<?> other = (Binding<?>) obj;
		return locator.equalTo(other.locator) && source.equalTo(other.source)
			&& scope.equalTo(other.scope) && type == other.type;
	}

	@Override
	public int hashCode() {
		return locator.hashCode() ^ source.hashCode();
	}

	@Override
	public int compareTo(Binding<?> other) {
		int res = locator.type().rawType.getCanonicalName().compareTo(
				other.locator.type().rawType.getCanonicalName());
		if (res != 0)
			return res;
		res = Qualifying.compare(locator.instance, other.locator.instance);
		if (res != 0)
			return res;
		res = Qualifying.compare(locator.target, other.locator.target);
		if (res != 0)
			return res;
		res = Qualifying.compare(source, other.source);
		if (res != 0)
			return res;
		return -1; // keep order
	}

	/**
	 * Removes those bindings that are ambiguous but also do not clash because
	 * of different {@link DeclarationType}s that replace each other.
	 */
	public static Binding<?>[] disambiguate(Binding<?>[] bindings) {
		if (bindings.length <= 1)
			return bindings;
		List<Binding<?>> uniques = new ArrayList<>(bindings.length);
		Arrays.sort(bindings);
		uniques.add(bindings[0]);
		int lastUniqueIndex = 0;
		Set<Type<?>> required = new HashSet<>();
		List<Binding<?>> dropped = new ArrayList<>();
		for (int i = 1; i < bindings.length; i++) {
			Binding<?> lastUnique = bindings[lastUniqueIndex];
			Binding<?> current = bindings[i];
			final boolean equalResource = lastUnique.locator.equalTo(
					current.locator);
			DeclarationType lastType = lastUnique.source.declarationType;
			DeclarationType curType = current.source.declarationType;
			if (equalResource && lastType.clashesWith(curType))
				throw InconsistentBinding.clash(lastUnique, current);
			if (curType == DeclarationType.REQUIRED) {
				required.add(current.locator.type());
			} else if (equalResource && (lastType.droppedWith(curType))) {
				if (isDuplicateIdenticalConstant(equalResource, lastUnique,
						current)) {
					dropped.add(current);
				} else {
					if (i - 1 == lastUniqueIndex)
						dropped.add(uniques.remove(uniques.size() - 1));
					dropped.add(current);
				}
			} else if (!equalResource || !curType.replacedBy(lastType)) {
				if (current.source.declarationType == DeclarationType.MULTI
					&& isDuplicateIdenticalConstant(equalResource, lastUnique,
							current)) {
					dropped.add(current);
				} else {
					uniques.add(current);
					lastUniqueIndex = i;
				}
			}
		}
		return withoutProvidedThatAreNotRequiredIn(uniques, required, dropped);
	}

	private static boolean isDuplicateIdenticalConstant(boolean equalResource,
			Binding<?> lastUnique, Binding<?> current) {
		return equalResource && current.type == BindingType.PREDEFINED
			&& lastUnique.supplier.equals(current.supplier);
	}

	private static Binding<?>[] withoutProvidedThatAreNotRequiredIn(
			List<Binding<?>> bindings, Set<Type<?>> required,
			List<Binding<?>> dropped) {
		List<Binding<?>> res = new ArrayList<>(bindings.size());
		for (Binding<?> b : bindings) {
			Type<?> type = b.locator.type();
			if (b.source.declarationType != DeclarationType.PROVIDED
				|| required.contains(type)) {
				res.add(b);
				required.remove(type);
			}
		}
		if (!required.isEmpty())
			throw new UnresolvableDependency.NoResourceForDependency(required,
					dropped);
		return arrayOf(res, Binding.class);
	}

}