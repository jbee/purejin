package se.jbee.inject.convert;

import static se.jbee.inject.Type.classType;
import static se.jbee.inject.config.NamesBy.defaultName;
import static se.jbee.inject.config.NamesBy.memberNameOr;

import se.jbee.inject.Converter;
import se.jbee.inject.Type;
import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.config.NamesBy;
import se.jbee.inject.config.ProducesBy;
import se.jbee.inject.config.ScopesBy;
import se.jbee.inject.config.SharesBy;

public abstract class ConverterModule extends BinderModule {

	@SuppressWarnings("rawtypes")
	private static final Type<Converter> ANY_CONVERTER_TYPE = classType(
			Converter.class);

	private static final NamesBy NAME_BY = memberNameOr(defaultName);
	private static final ProducesBy PRODUCES_BY = ProducesBy.declaredMethods //
			.returnTypeAssignableTo(ANY_CONVERTER_TYPE);
	private static final SharesBy SHARES_BY = SharesBy.declaredFields //
			.typeAssignableTo(ANY_CONVERTER_TYPE);

	@Override
	protected void declare() {
		autobindConverters().in(this);
	}

	private AutoBinder autobindConverters() {
		return autobind() //
				.nameBy(NAME_BY) //
				.scopeBy(ScopesBy.type) //
				.shareBy(SHARES_BY) //
				.produceBy(PRODUCES_BY);
	}

}
