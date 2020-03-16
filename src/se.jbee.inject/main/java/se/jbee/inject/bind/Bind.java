/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.DeclarationType;
import se.jbee.inject.Env;
import se.jbee.inject.Instance;
import se.jbee.inject.Locator;
import se.jbee.inject.Name;
import se.jbee.inject.Scope;
import se.jbee.inject.Source;
import se.jbee.inject.Target;
import se.jbee.inject.config.ScopesBy;
import se.jbee.inject.container.Supplier;
import se.jbee.inject.declare.Binding;
import se.jbee.inject.declare.BindingType;
import se.jbee.inject.declare.Bindings;

/**
 * The data and behavior used to create binds.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Bind {

	public static final Bind UNINITIALIZED = new Bind(null, null, null,
			Scope.mirror, Target.ANY);

	public final Env env;
	public final Bindings bindings;
	public final Source source;
	public final Name scope;
	public final Target target;

	private Bind(Env env, Bindings bindings, Source source, Name scope,
			Target target) {
		this.env = env;
		this.bindings = bindings;
		this.source = source;
		this.scope = scope;
		this.target = target;
	}

	public Bind asMulti() {
		return as(DeclarationType.MULTI);
	}

	public Bind asAuto() {
		return as(DeclarationType.AUTO);
	}

	public Bind asImplicit() {
		return as(DeclarationType.IMPLICIT);
	}

	public Bind asDefault() {
		return as(DeclarationType.DEFAULT);
	}

	public Bind asRequired() {
		return as(DeclarationType.REQUIRED);
	}

	public Bind asProvided() {
		return as(DeclarationType.PROVIDED);
	}

	public Bind as(DeclarationType type) {
		return with(source.typed(type));
	}

	public Bind per(Name scope) {
		return new Bind(env, bindings, source, scope, target);
	}

	public Bind with(Target target) {
		return new Bind(env, bindings, source, scope, target);
	}

	public Bind into(Env env, Bindings bindings) {
		return new Bind(env, bindings, source, scope, target);
	}

	public Bind with(Source source) {
		return new Bind(env, bindings, source, scope, target);
	}

	public Bind within(Instance<?> parent) {
		return new Bind(env, bindings, source, scope, target.within(parent));
	}

	public Bind next() {
		return source == null
			? this
			: new Bind(env, bindings, source.next(), scope, target);
	}

	public <T> Binding<T> asType(Locator<T> locator, BindingType type,
			Supplier<? extends T> supplier) {
		return Binding.binding(locator, type, supplier, effectiveScope(locator),
				source);
	}

	private <T> Name effectiveScope(Locator<T> locator) {
		Name effectiveScope = scope.equalTo(Scope.mirror)
			? env.property(ScopesBy.class, source.pkg()).reflect(
					locator.type().rawType)
			: scope;
		return effectiveScope.equalTo(ScopesBy.auto)
			? Scope.application
			: effectiveScope;
	}
}
