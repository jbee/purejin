package de.jbee.inject;

public interface ModuleBinder {

	//TODO I guess we start with a root Bundle
	Binding<?>[] bind( Class<? extends Bundle> root );
}
