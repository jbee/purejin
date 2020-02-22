package com.example.app;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;

import se.jbee.inject.Scope;
import se.jbee.inject.bind.BinderModuleWith;

/**
 * In contrast to {@link ServiceAnnotation} this will be bound to the annotation
 * using {@link ServiceLoader} so this "implementation" is annotated with the
 * {@link Annotation} it implemented which links it to that annotation after it
 * is instantiated by the {@link ServiceLoader}.
 */
@Support
public class SupportAnnotation extends BinderModuleWith<Class<?>> {

	@Override
	protected void declare(Class<?> annotated) {
		per(Scope.application).autobind(annotated).toConstructor();
	}
}