package se.jbee.inject.action;

import se.jbee.inject.*;
import se.jbee.inject.config.HintsBy;
import se.jbee.inject.config.Invoke;
import se.jbee.lang.Type;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.lang.Type.actualReturnType;

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
	public final ActionTarget target;
	public final Type<A> in;
	public final Type<B> out;
	private final AtomicBoolean isDisconnected = new AtomicBoolean();
	private final Runnable onDisconnect;
	private final InjectionSite injection;
	private final int inputIndex;

	public ActionSite(ActionTarget target, Type<A> in, Type<B> out,
			Injector context, Consumer<ActionSite<?, ?>> onDisconnect) {
		this.target = target;
		this.in = in;
		this.out = out;
		this.onDisconnect = () -> onDisconnect.accept(ActionSite.this);
		Env env = context.resolve(Env.class).in(ActionModule.class);
		Hint<A> inArg = Hint.constantNull(in);
		Hint<?>[] actualParameters = env.property(HintsBy.class)
				.applyTo(context, target.action, target.as, inArg);
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

	public Object[] args(Injector context, Object input) {
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
			Method action = target.action;
				return out.rawType.cast(
						target.invoke.call(action, target.instance, args));
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
		return in + " => " + out + " [" + target.action.toGenericString() + "]";
	}

	public static final class ActionTarget {

		public final Object instance;
		public final Type<?> as;
		public final Method action;
		public final Invoke invoke;

		ActionTarget(Object instance, Type<?> as, Method action, Invoke invoke) {
			this.instance = instance;
			this.as = as;
			this.action = action;
			this.invoke = invoke;
		}

		public Type<?> returnType() {
			return actualReturnType(action, as);
		}

		public <A, B> boolean isUsableFor(Type<A> in, Type<B> out) {
			if (!returnType().equalTo(out))
				return false;
			if (in.equalTo(Type.VOID) && action.getParameterCount() == 0)
				return true;
			for (Parameter p : action.getParameters()) {
				if (Type.actualParameterType(p, as).equalTo(in))
					return true;
			}
			return false;
		}
	}
}
