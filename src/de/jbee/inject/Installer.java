package de.jbee.inject;

/**
 * The basic idea is to split the bind-instructing process into 2 steps: installing modules and
 * instruct bindings in the installed modules.
 * 
 * Thereby it is possible to keep track of the modules that should be installed before actually
 * install them. This has two major benefits:
 * 
 * 1. it is possible and intentional to declare installation of the same module as often as wanted
 * or needed without actually installing them more then once. This allows to see other modules as
 * needed dependencies or 'parent'-modules.
 * 
 * 2. the installation can be the first step of the verification (in a unit-test). The binding can
 * be omitted so that overall test of a configuration can be very fast.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public interface Installer {

	void install( Module module );
}
