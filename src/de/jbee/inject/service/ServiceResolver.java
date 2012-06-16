package de.jbee.inject.service;

import de.jbee.inject.Type;

public interface ServiceResolver {

	<P, R> Service<P, R> resolve( Type<P> parameterType, Type<R> returnType );
}
