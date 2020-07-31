package se.jbee.inject.convert;

import static se.jbee.inject.Type.classType;
import static se.jbee.inject.config.NamesBy.defaultName;
import static se.jbee.inject.config.NamesBy.memberNameOr;

import se.jbee.inject.Converter;
import se.jbee.inject.Type;
import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.config.ProducesBy;
import se.jbee.inject.config.ScopesBy;
import se.jbee.inject.config.SharesBy;

public abstract class ConverterModule extends BinderModule {

	@Override
	protected void declare() {
		@SuppressWarnings("rawtypes")
		Type<Converter> anyConverter = classType(Converter.class);
		autobind() //
				.nameBy(memberNameOr(defaultName)) //
				.scopeBy(ScopesBy.type) //
				.shareBy(SharesBy.declaredFields.typeAssignableTo(anyConverter)) //
				.produceBy(ProducesBy.declaredMethods.returnTypeAssignableTo(
						anyConverter)) // 
				.in(this);
	}

}
