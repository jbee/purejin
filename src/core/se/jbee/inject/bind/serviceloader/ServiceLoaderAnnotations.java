package se.jbee.inject.bind.serviceloader;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;

import se.jbee.inject.Env;
import se.jbee.inject.Name;
import se.jbee.inject.Type;
import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.bootstrap.Environment;
import se.jbee.inject.declare.Extends;
import se.jbee.inject.declare.Module;
import se.jbee.inject.declare.ModuleWith;

/**
 * A {@link Module} that is meant to be installed during the bootstrapping of a
 * custom {@link Env}. It loads all {@link ModuleWith} from
 * {@link ServiceLoader} which define the effect of an particular type level
 * {@link Annotation}. That {@link Annotation} is either referred to by
 * annotated the {@link ModuleWith} implementation class with the
 * {@link Extends} annotation (use {@link Extends#value()}) or by annotated the
 * implementation class with the target {@link Annotation} itself. In that case
 * that {@link Annotation} must be the only runtime annotation present.
 * 
 * @since 19.1
 */
public class ServiceLoaderAnnotations extends BinderModule {

	@Override
	protected void declare() {
		for (ModuleWith<?> def : ServiceLoader.load(ModuleWith.class)) {
			@SuppressWarnings("rawtypes")
			Type<? extends ModuleWith> genericModuleType = Type.supertype(
					ModuleWith.class, Type.raw(def.getClass()));
			if (genericModuleType.parameter(0).rawType == Class.class) {
				@SuppressWarnings("unchecked")
				ModuleWith<Class<?>> effect = (ModuleWith<Class<?>>) def;
				if (def.getClass().isAnnotationPresent(Extends.class)) {
					Class<?> type = def.getClass().getAnnotation(
							Extends.class).value();
					if (type.isAnnotation()) {
						@SuppressWarnings("unchecked")
						Class<? extends Annotation> target = (Class<? extends Annotation>) type;
						bind(target, effect);
					}
				} else {
					Annotation[] annotations = def.getClass().getAnnotations();
					if (annotations.length == 1) {
						bind(annotations[0].annotationType(), effect);
					}
				}
			}
		}
	}

	protected final void bind(Class<? extends Annotation> name,
			ModuleWith<Class<?>> value) {
		bind(Name.named(name), Environment.ANNOTATION).to(value);
	}
}
