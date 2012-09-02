package de.jbee.inject.bind;

public interface ModuleTree {

	Module[] installed( Class<? extends Bundle> root );
}
