/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.DeclarationType;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Source;
import se.jbee.inject.Target;
import se.jbee.inject.bootstrap.Binding;
import se.jbee.inject.bootstrap.BindingType;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.config.ScopingMirror;
import se.jbee.inject.container.Supplier;

/**
 * The data and behavior used to create binds.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Bind {

	public static Bind create(Bindings bindings, Source source) {
		return new Bind(bindings, source, Scope.mirror, Target.ANY);
	}

	public final Bindings bindings;
	public final Source source;
	public final Name scope;
	public final Target target;

	private Bind(Bindings bindings, Source source, Name scope, Target target) {
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
		return new Bind(bindings, source, scope, target);
	}

	public Bind with(Target target) {
		return new Bind(bindings, source, scope, target);
	}

	public Bind into(Bindings bindings) {
		return new Bind(bindings, source, scope, target);
	}

	public Bind with(Source source) {
		return new Bind(bindings, source, scope, target);
	}

	public Bind within(Instance<?> parent) {
		return new Bind(bindings, source, scope, target.within(parent));
	}

	public Bind next() {
		return source == null
			? this
			: new Bind(bindings, source.next(), scope, target);
	}

	public <T> Binding<T> asType(Resource<T> resource, BindingType type,
			Supplier<? extends T> supplier) {
		Name effectiveScope = scope.equalTo(Scope.mirror)
			? bindings.mirrors.scoping.reflect(resource.type().rawType)
			: scope;
		if (effectiveScope.equalTo(ScopingMirror.auto))
			effectiveScope = Scope.application;
		return Binding.binding(resource, type, supplier, effectiveScope,
				source);
	}
}
