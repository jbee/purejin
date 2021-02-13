package se.jbee.inject.defaults;

import se.jbee.inject.*;
import se.jbee.inject.UnresolvableDependency.ResourceResolutionFailed;
import se.jbee.inject.bind.Binding;
import se.jbee.inject.bind.BindingType;
import se.jbee.inject.bind.InconsistentBinding;
import se.jbee.lang.Type;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.binarySearch;
import static se.jbee.lang.Utils.arrayOf;

/**
 * Is the default implementation for {@link se.jbee.inject.bind.BindingConsolidation}.
 * <p>
 * It does not implement the interface since it has no state. Use a method
 * reference lambda on {@link #consolidate(Env, Binding[])} instead.
 *
 * @since 8.1
 */
public final class DefaultBindingConsolidation {

	private DefaultBindingConsolidation() {
		throw new UnsupportedOperationException("use lambda on consolidate");
	}

	/**
	 * Removes those bindings that are ambiguous but also do not clash because
	 * of different {@link DeclarationType}s that replace each other.
	 */
	public static Binding<?>[] consolidate(Env env, Binding<?>[] bindings) {
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
			final boolean equalResource = lastUnique.signature.equalTo(
					current.signature);
			DeclarationType lastType = lastUnique.source.declarationType;
			DeclarationType curType = current.source.declarationType;
			if (equalResource && lastType.clashesWith(curType))
				throw InconsistentBinding.clash(lastUnique, current);
			if (curType == DeclarationType.REQUIRED) {
				required.add(current.signature.type());
			} else if (equalResource && (lastType.droppedWith(curType))) {
				if (!isDuplicateIdenticalConstant(true, lastUnique,
						current) && i - 1 == lastUniqueIndex) {
					dropped.add(uniques.remove(uniques.size() - 1));
				}
				dropped.add(current);
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
		return withoutProvidedThatAreNotRequiredIn(env, uniques, required, dropped);
	}

	private static boolean isDuplicateIdenticalConstant(boolean equalResource,
			Binding<?> lastUnique, Binding<?> current) {
		return equalResource && current.type == BindingType.PREDEFINED
				&& lastUnique.supplier.equals(current.supplier);
	}

	private static Binding<?>[] withoutProvidedThatAreNotRequiredIn(Env env,
			List<Binding<?>> bindings, Set<Type<?>> required,
			List<Binding<?>> dropped) {
		List<Binding<?>> res = new ArrayList<>(bindings.size());
		for (Binding<?> b : bindings) {
			Type<?> type = b.signature.type();
			if (b.source.declarationType != DeclarationType.PROVIDED
					|| required.contains(type)) {
				res.add(b);
				required.remove(type);
			}
		}
		if (!required.isEmpty())
			throw new ResourceResolutionFailed(required, dropped);
		return env.property(Env.BIND_BINDINGS, false)
			   ? withListItselfInserted(res)
			   : arrayOf(res, Binding.class);
	}

	/**
	 * This must look very confusing at first. What we want to do here is add a
	 * {@link Binding} that has all the bindings as array including itself. But
	 * this must be inserted at the correct sorting position. Because of the
	 * hen-egg situation with the array itself we use {@link AtomicReference} as
	 * a box that we can fill later.
	 */
	private static Binding<?>[] withListItselfInserted(List<Binding<?>> res) {
		AtomicReference<Binding<?>[]> box = new AtomicReference<>();
		Binding<?> self = Binding.binding(Locator.locator(Binding[].class),
				BindingType.PREDEFINED, (dep, context) -> box.get(),
				Scope.container, Source.source(Binding.class));
		int insertIndex = -binarySearch(res, self, Binding::compareTo) - 1;
		res.add(insertIndex, self);
		Binding<?>[] array = arrayOf(res, Binding.class);
		box.set(array);
		return array;
	}
}
