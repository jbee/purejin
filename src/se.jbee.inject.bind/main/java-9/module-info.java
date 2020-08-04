module se.jbee.inject.bind {

	requires se.jbee.inject.api;

	exports se.jbee.inject.bind;
	exports se.jbee.inject.binder;
	exports se.jbee.inject.config;


	uses se.jbee.inject.bind.Bundle;
	uses se.jbee.inject.bind.ModuleWith;
}
