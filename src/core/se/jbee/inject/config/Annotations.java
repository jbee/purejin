package se.jbee.inject.config;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import se.jbee.inject.Link;
import se.jbee.inject.declare.ModuleWith;

/**
 * Configuration for custom annotations. These are annotations which in their
 * effect are defined by the user in form of a {@link ModuleWith} where the
 * annotated class is passed to the implementation as parameter and the
 * implementation does the binds.
 * 
 * @since 19.1
 */
@Deprecated
public final class Annotations {

	public static final Annotations DETECT = new Annotations(true);

	/**
	 * When true the {@link ServiceLoader} is used to find {@link ModuleWith}
	 * implementations that implement a custom annotation's effects on an
	 * annotated {@link Class}.
	 */
	public final boolean detect;

	private final AtomicBoolean hasDetected;
	private final Map<Class<? extends Annotation>, ModuleWith<Class<?>>> typeAnnotations;

	private Annotations(boolean detect) {
		this(detect, new AtomicBoolean(), new HashMap<>());
	}

	private Annotations(boolean detect, AtomicBoolean hasDetected,
			Map<Class<? extends Annotation>, ModuleWith<Class<?>>> typeAnnotations) {
		this.detect = detect;
		this.hasDetected = hasDetected;
		this.typeAnnotations = typeAnnotations;
	}

	public Annotations detect(boolean detect) {
		if (detect == this.detect)
			return this;
		return new Annotations(detect, new AtomicBoolean(hasDetected.get()),
				typeAnnotations);
	}

	public Annotations declare(Class<? extends Annotation> type,
			ModuleWith<Class<?>> as) {
		Map<Class<? extends Annotation>, ModuleWith<Class<?>>> clone = new HashMap<>(
				typeAnnotations);
		clone.put(type, as);
		return new Annotations(detect, new AtomicBoolean(hasDetected.get()),
				clone);
	}

	public ModuleWith<Class<?>> effect(Class<? extends Annotation> type) {
		ensureAnnotationDetectionIsDone();
		return typeAnnotations.get(type);
	}

	private void ensureAnnotationDetectionIsDone() {
		if (!detect)
			return;
		if (hasDetected.compareAndSet(false, true)) {
			detectAnnotations();
		}
	}

	private void detectAnnotations() {
		for (ModuleWith<?> def : ServiceLoader.load(ModuleWith.class)) {
			if (def.getClass().isAnnotationPresent(Link.class)) {
				declareDetected(def.getClass().getAnnotation(Link.class).to(),
						def);
			} else {
				Annotation[] annotations = def.getClass().getAnnotations();
				if (annotations.length == 1) {
					declareDetected(annotations[0].annotationType(), def);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void declareDetected(Class<?> type, ModuleWith<?> as) {
		if (type.isAnnotation()) {
			ModuleWith<Class<?>> effect = (ModuleWith<Class<?>>) as;
			typeAnnotations.put((Class<? extends Annotation>) type, effect);
		}
	}
}
