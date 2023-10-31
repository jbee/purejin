package se.jbee.inject.action;

import se.jbee.inject.*;
import se.jbee.inject.config.ConnectionTarget;
import se.jbee.inject.config.HintsBy;
import se.jbee.lang.Type;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static se.jbee.inject.Dependency.dependency;

/**
 * Describes a unique action implementation point. That is the particular
 * {@link Method} that implements the {@link Action} for the input parameter
 * {@link Type}.
 *
 * @param <A> Type of the input parameter
 * @param <B> Type of the output value
 *
 * @since 8.1
 */
public final class ActionSite<A, B> {

	/**
	 * The instance implementing the {@link Action} {@link Method}.
	 */
	public final ConnectionTarget target;
	public final Type<A> in;
	public final Type<B> out;
	private final AtomicBoolean isDisconnected = new AtomicBoolean();
	private final Runnable onDisconnect;
	private final InjectionSite injection;
	private final int inputIndex;

	public ActionSite(ConnectionTarget target, Type<A> in, Type<B> out,
			Injector context, Consumer<ActionSite<?, ?>> onDisconnect) {
		this.target = target;
		this.in = in;
		this.out = out;
		this.onDisconnect = () -> onDisconnect.accept(this);
		Env env = context.resolve(Env.class).in(ActionSite.class);
		Hint<A> inArg = Hint.constantNull(in);
		Hint<?>[] actualParameters = env.property(HintsBy.class)
				.applyTo(context, target.connected, target.as, inArg);
		this.injection = new InjectionSite(context,
				dependency(out).injectingInto(target.as), actualParameters);
		this.inputIndex = asList(actualParameters).indexOf(inArg);
	}

	private void disconnect() {
		if (isDisconnected.compareAndSet(false, true))
			onDisconnect.run();
	}

	public boolean isDisconnected() {
		return isDisconnected.get();
	}

	public Object[] args(Injector context, A input) {
		Object[] args;
		try {
			args = injection.args(context);
		} catch (UnresolvableDependency e) {
			throw new ActionExecutionFailed(
					"Failed to provide all implicit arguments", e);
		}
		if (inputIndex >= 0)
			args[inputIndex] = input;
		return args;
	}

	public B call(Object[] args, Consumer<Exception> errorHandler) throws ActionExecutionFailed {
		if (isDisconnected())
			throw new DisconnectException("Action already disconnected.");
		try {
				return out.rawType.cast(
						target.invoke.call(target.connected, target.instance, args));
		} catch (InvocationTargetException ex) {
			if (ex.getTargetException() instanceof DisconnectException) {
				disconnect();
				throw (DisconnectException) ex.getTargetException();
			}
			if (errorHandler != null)
				errorHandler.accept((Exception) ex.getCause());
			throw new ActionExecutionFailed(
					"Exception on invocation of the action", ex.getCause());
		} catch (Exception ex) {
			if (errorHandler != null)
				errorHandler.accept(ex);
			throw new ActionExecutionFailed(
					"Exception on invocation of the action", ex);
		}
	}

	@Override
	public String toString() {
		return in + " => " + out + " [" + target.connected.toGenericString() + "]";
	}

}
