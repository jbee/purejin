package se.jbee.inject.binder.spi;

import se.jbee.inject.config.ProducesBy;

public interface ConnectInBinder<B> {

	B connect(ProducesBy connectsBy);
}
