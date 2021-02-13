/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.Env;
import se.jbee.inject.InconsistentDeclaration;
import se.jbee.inject.bind.*;
import se.jbee.inject.binder.Binder.RootBinder;

import java.lang.annotation.Annotation;

import static se.jbee.lang.Type.raw;

/**
 * A {@link RootBinder} that can be initialized using the
 * {@link #__init__(Env, Bindings)} method.
 *
 * This allows to change the start {@link Bind} once.
 */
public abstract class AbstractBinderModule extends RootBinder
		implements Bundle {

	private Bind bind;
	private Boolean declaring;

	protected AbstractBinderModule() {
		super(Bind.UNINITIALIZED);
		this.bind = super.bind();
	}

	@Override
	public final Bind bind() {
		return bind;
	}

	protected final void __init__(Env env, Bindings bindings) {
		InconsistentBinding.nonnullThrowsReentranceException(declaring);
		this.bind = init(bind.into(env, bindings));
		declaring = true;
	}

	protected Bind init(Bind bind) {
		return bind;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected static void installAnnotated(Class<? extends Bundle> bundle,
			Bootstrapper bootstrap) {
		for (Installs installs : bundle.getAnnotationsByType(Installs.class)) {
			for (Class<? extends Bundle> b : installs.bundles()) {
				bootstrap.install(b);
			}
			if (installs.features() != Enum.class) {
				Class<?> features = installs.features();
				if (!raw(features).isAssignableTo(raw(Dependent.class)))
					throw new InconsistentDeclaration(
							"@Installs#features() Class must be compatible with <F extends Enum<F> & Toggled<F>>");
				Class<? extends Annotation> by = installs.by();
				Annotation selection = by == Annotation.class
						? null
						: bundle.getAnnotation(by);
				install(bootstrap, (Class) features, selection);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <E extends Enum<E> & Dependent<E>> void install(
			Bootstrapper bootstrap, Class<E> features, Annotation selection) {
		if (selection == null) {
			bootstrap.install(features.getEnumConstants());
		} else {
			try {
				Object[] value = (Object[]) selection.getClass()
						.getMethod("value")
						.invoke(selection);
				if (value.length == 0)
					return;
				if (value[0].getClass() != features)
					throw new InconsistentDeclaration(
							"The annotation referenced by @Installs#by() must have the same component enum type as @Installs#s#features()");
				bootstrap.install((E[]) value);
			} catch (Exception ex) {
				throw new InconsistentDeclaration(ex);
			}
		}
	}
}
