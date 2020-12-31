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
import se.jbee.inject.lang.Utils;

import static se.jbee.inject.lang.Type.raw;

/**
 * A {@link RootBinder} that can be initialized using the
 * {@link #__init__(Env, Bindings)} method.
 *
 * This allows to change the start {@link Bind} once.
 */
public abstract class InitializedBinder extends RootBinder {

	private Bind bind;
	private Boolean initialized;

	protected InitializedBinder() {
		super(Bind.UNINITIALIZED);
		this.bind = super.bind();
	}

	@Override
	public final Bind bind() {
		return bind;
	}

	protected final void __init__(Env env, Bindings bindings) {
		InconsistentBinding.nonnullThrowsReentranceException(initialized);
		this.bind = init(bind.into(env, bindings));
		initialized = true;
	}

	protected Bind init(Bind bind) {
		return bind;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected final void installAnnotated(Class<? extends Bundle> bundle,
			Bootstrapper bootstrap) {
		if (bundle.isAnnotationPresent(Installs.class)) {
			Installs installs = bundle.getAnnotation(Installs.class);
			for (Class<? extends Bundle> b : installs.bundles()) {
				bootstrap.install(b);
			}
			if (installs.features() != Enum.class) {
				Class<?> features = installs.features();
				if (!raw(features).isAssignableTo(raw(Dependent.class)))
					throw new InconsistentDeclaration(
							"@Installs feature Class must be compatible with <F extends Enum<F> & Toggled<F>>");
				install(bootstrap, (Class) features, installs.selection());
			}
		}
	}

	private <F extends Enum<F> & Dependent<F>> void install(
			Bootstrapper bootstrap, Class<F> features, String[] names) {
		if (names.length == 0) {
			bootstrap.install(features.getEnumConstants());
		} else {
			F[] installed = Utils.newArray(features, names.length);
			for (int i = 0; i < names.length; i++)
				installed[i] = Enum.valueOf(features, names[i]);
			bootstrap.install(installed);
		}
	}
}
