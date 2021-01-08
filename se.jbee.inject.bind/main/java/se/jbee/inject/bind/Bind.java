/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.*;
import se.jbee.inject.config.ScopesBy;

/**
 * The data and behavior used to create binds.
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
		this.env = source == null || env == null ? env : env.in(source.ident);
		this.bindings = bindings;
		this.source = source;
		this.scope = scope;
		this.target = target;
	}

	public Bind asMulti() {
		return as(DeclarationType.MULTI);
	}

	public Bind asPublished() {
		return as(DeclarationType.PUBLISHED);
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

	public Bind into(Env env, Bindings bindings) {
		return new Bind(env, bindings, source, scope, target);
	}

	public Bind with(Target target) {
		return new Bind(env, bindings, source, scope, target);
	}

	public Bind with(Env env) {
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
		Name effectiveScope = scope.equalTo(Scope.mirror) //
				? env.property(ScopesBy.class)
					.reflect(locator.type().rawType)
				: scope;
		return effectiveScope.equalTo(Scope.auto)
			? Scope.application
			: effectiveScope;
	}
}
