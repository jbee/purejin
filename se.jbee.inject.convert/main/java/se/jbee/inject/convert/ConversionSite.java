package se.jbee.inject.convert;

import se.jbee.inject.*;
import se.jbee.inject.config.ConnectionTarget;
import se.jbee.inject.config.HintsBy;
import se.jbee.lang.Type;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static se.jbee.inject.Dependency.dependency;

/**
 * A {@link Converter} that originates from connecting a {@link Method}.
 * <p>
 * The single parameter of the method becomes the input {@link Type}. The return
 * type of the method the output {@link Type}.
 */
public final class ConversionSite<A, B> implements Converter<A, B> {

	final ConnectionTarget target;
	final Type<A> from;
	final Type<B> to;
	private final AtomicBoolean isDisconnected = new AtomicBoolean();
	private final Runnable onDisconnect;
	private final InjectionSite injection;
	private final int inputIndex;
	private final Injector context;

	public ConversionSite(ConnectionTarget target, int fromIndex, Type<A> from, Type<B> to,
			 Injector context, Consumer<ConversionSite<?, ?>> onDisconnect) {
		this.target = target;
		this.from = from;
		this.to = to;
		this.onDisconnect = () -> onDisconnect.accept(this);
		this.inputIndex = fromIndex;
		this.context = context;
		Env env = context.resolve(Env.class).in(ConversionSite.class);
		Hint<A> inArg = Hint.constantNull(from);
		Hint<?>[] actualParameters = env.property(HintsBy.class)
				.applyTo(context, target.connected, target.as);
		actualParameters[inputIndex] = inArg;
		this.injection = new InjectionSite(context,
				dependency(to).injectingInto(target.as), actualParameters);
	}

	private void disconnect() {
		if (isDisconnected.compareAndSet(false, true))
			onDisconnect.run();
	}

	public boolean isDisconnected() {
		return isDisconnected.get();
	}

	public Object[] args(A input) {
		Object[] args;
		try {
			args = injection.args(context);
		} catch (UnresolvableDependency ex) {
			throw new IllegalArgumentException(ex);
		}
		if (inputIndex >= 0)
			args[inputIndex] = input;
		return args;
	}

	@Override
	public B convert(A input) {
		if (isDisconnected())
			throw new DisconnectException("Converter already disconnected.");
		try {
			return to.rawType.cast(
					target.invoke.call(target.connected, target.instance, args(input)));
		} catch (IllegalArgumentException ex) {
			throw ex;
		} catch (InvocationTargetException ex) {
			if (ex.getTargetException() instanceof DisconnectException) {
				disconnect();
				throw (DisconnectException) ex.getTargetException();
			}
			throw new IllegalArgumentException(ex.getTargetException());
		} catch (Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	@Override
	public String toString() {
		return from + " => " + to + " [" + target.connected.toGenericString() + "]";
	}
}
