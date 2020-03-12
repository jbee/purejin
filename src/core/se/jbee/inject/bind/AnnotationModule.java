package se.jbee.inject.bind;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;

import se.jbee.inject.Link;
import se.jbee.inject.Type;
import se.jbee.inject.declare.ModuleWith;

public abstract class AnnotationModule extends BinderModule {

	protected final void bind(Class<? extends Annotation> typeAnnotation,
			ModuleWith<Class<?>> as) {

	}

	@SuppressWarnings("unchecked")
	private void detectAnnotations() {
		for (ModuleWith<?> def : ServiceLoader.load(ModuleWith.class)) {
			Type<? extends ModuleWith> genericModuleType = Type.supertype(
					ModuleWith.class, Type.raw(def.getClass()));
			if (def.getClass().isAnnotationPresent(Link.class)) {
				Class<?> type = def.getClass().getAnnotation(Link.class).to();
				if (type.isAnnotation()) {
					declareDetected((Class<? extends Annotation>) type, def);
				}
			} else {
				Annotation[] annotations = def.getClass().getAnnotations();
				if (annotations.length == 1) {
					declareDetected(annotations[0].annotationType(), def);
				}
			}
		}
	}

	private void declareDetected(Class<? extends Annotation> type,
			ModuleWith<?> as) {
		if (type.isAnnotation()) {
			ModuleWith<Class<?>> effect = (ModuleWith<Class<?>>) as;
			//
		}
	}
}
