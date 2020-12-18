package se.jbee.inject.convert;

import se.jbee.inject.Converter;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.config.NamesBy;
import se.jbee.inject.config.ProducesBy;
import se.jbee.inject.config.ScopesBy;
import se.jbee.inject.config.SharesBy;
import se.jbee.inject.lang.Type;

import static se.jbee.inject.lang.Type.classType;

/**
 * Base class for {@link se.jbee.inject.bind.Module}s that are used to bind
 * {@link Converter}s.
 * <p>
 * By default it is assumed that the converters are defined as fields or methods
 * within the class extending the {@link ConverterModule}.
 * <p>
 * Alternatively this base class can be used by overriding {@link #declare()}
 * and calling {@link #autobindConverters()} with other target types.
 * <p>
 * Last but not least this can just act as a manual on how to bind converters
 * using {@link #autobind()} or other manual binds.
 *
 * @since 8.1
 */
public abstract class ConverterModule extends BinderModule {

	@SuppressWarnings("rawtypes")
	private static final Type<Converter> ANY_CONVERTER_TYPE = classType(
			Converter.class);

	private static final ProducesBy PRODUCES_BY = ProducesBy.OPTIMISTIC //
			.returnTypeAssignableTo(ANY_CONVERTER_TYPE);
	private static final SharesBy SHARES_BY = SharesBy.declaredFields //
			.typeAssignableTo(ANY_CONVERTER_TYPE);

	@Override
	protected void declare() {
		autobindConverters().in(this);
	}

	protected final AutoBinder autobindConverters() {
		return autobind() //
				.nameBy(NamesBy.DECLARED_NAME) //
				.scopeBy(ScopesBy.RETURN_TYPE) //
				.shareBy(SHARES_BY) //
				.produceBy(PRODUCES_BY);
	}

}
