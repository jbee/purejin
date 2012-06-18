package de.jbee.inject.service;

import de.jbee.inject.Type;

public interface ServiceProvider {

	<P, R> ServiceMethod<P, R> provide( Type<P> parameterType, Type<R> returnType );
}
