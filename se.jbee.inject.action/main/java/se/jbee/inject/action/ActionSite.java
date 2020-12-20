package se.jbee.inject.action;

import se.jbee.inject.Hint;
import se.jbee.inject.InjectionSite;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.lang.Reflect;
import se.jbee.inject.lang.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.lang.Type.actualParameterType;
import static se.jbee.inject.lang.Type.actualReturnType;
import static se.jbee.inject.lang.Utils.arrayMap;

/**
 * Describes a unique action implementation point. That is the particular
 * {@link Method} that implements the {@link Action} for the input parameter
 * {@link Type}.
 *
 * @author Jan Bernitt
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
	private final InjectionSite injection;
	private final int inputIndex;

	public ActionSite(ActionTarget target, Type<A> in, Type<B> out,
			Injector context) {
		this.target = target;
		this.in = in;
		this.out = out;
		Type<?>[] types = arrayMap(target.action.getParameters(), Type.class,
				p -> actualParameterType(p, target.as));
		this.injection = new InjectionSite(context,
				dependency(out).injectingInto(target.as),
				Hint.match(types, Hint.constantNull(in)));
		this.inputIndex = asList(types).indexOf(in);
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
		try {
			Method action = target.action;
			return out.rawType.cast(
					Reflect.produce(action, target.instance, args,
					e -> UnresolvableDependency.SupplyFailed.valueOf(e, action)));
		} catch (UnresolvableDependency.SupplyFailed e) {
			Exception ex = e;
			if (e.getCause() instanceof Exception) {
				ex = (Exception) e.getCause();
			}
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

		ActionTarget(Object instance, Type<?> as, Method action) {
			this.instance = instance;
			this.as = as;
			this.action = action;
		}

		public Type<?> returnType() {
			return actualReturnType(action, as);
		}

		public <A, B> boolean isApplicableFor(Type<A> in, Type<B> out) {
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
