package de.jbee.inject.bind;

public interface Bundler {

	Class<? extends Bundle>[] bundle( Class<? extends Bundle> root );
}
