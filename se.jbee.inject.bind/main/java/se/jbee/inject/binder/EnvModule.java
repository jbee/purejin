package se.jbee.inject.binder;

import se.jbee.inject.*;
import se.jbee.inject.bind.Bind;
import se.jbee.inject.config.PublishesBy;
import se.jbee.lang.Type;

import java.util.Set;

/**
 * Base {@link se.jbee.inject.bind.Module} for usage when declaring an {@link
 * Env}.
 * <p>
 * The important difference are:
 * <p>
 * {@link #installDefaults()} is {@code false}, meaning no "application context"
 * basics are installed. These are not used in an {@link Env}.
 * <p>
 * This class is annotated with {@link Extends} referring to {@link Env} which
 * means should the extending class be stated as provider of a {@link
 * se.jbee.inject.bind.Bundle} for the {@link java.util.ServiceLoader} it is
 * understood as belonging to the {@link Env}. This is a convention then picked
 * up by {@link ServiceLoaderEnvBundles}.
 *
 * When using {@link EnvModule} as base class the resulting binds are like
 * "globals" in the {@link Env}. To localise the effect of the binding to the
 * package of the module (and its sub-packages) use the {@link LocalEnvModule}
 * as base class.
 *
 * @see LocalEnvModule
 *
 * @since 8.1
 */
@Extends(Env.class)
public abstract class EnvModule extends BinderModule {

	@Override
	protected final boolean installDefaults() {
		return false;
	}

	@Override
	protected Bind init(Bind bind) {
		return bind.per(Scope.container);
	}

	/**
	 * Adds the provided {@link Class} as one that if implemented is published
	 * as API when bound as {@link Binder#withPublishedAccess()}.
	 * <p>
	 * Note that this only has a consequential effect if the general {@link
	 * PublishesBy} strategy uses {@link PublishesBy#declaredSet(Set)}, for
	 * example via {@link PublishesBy#liftDeclaredSet(PublishesBy, Type,
	 * Injector)}.
	 *
	 * @param api A type that should considered an API of implemented by a type
	 *            bound with {@link Binder#withPublishedAccess()}
	 */
	public final void addPublishedAPI(Class<?> api) {
		injectingInto(Env.class).plug(api).into(PublishesBy.class);
	}

	/**
	 * Bind the default {@link PublishesBy} strategy
	 */
	public final TypedBinder<PublishesBy> publish() {
		return bind(PublishesBy.class);
	}

	/**
	 * Bind the {@link PublishesBy} strategy for a specific implementation type
	 *
	 * @param implementation a type bound using {@link Binder#withPublishedAccess()}
	 */
	public final TypedBinder<PublishesBy> publish(Class<?> implementation) {
		return bind(Name.named(implementation), PublishesBy.class);
	}

}
