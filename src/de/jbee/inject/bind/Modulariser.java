package de.jbee.inject.bind;

public interface Modulariser {

	Module[] modularise( Class<? extends Bundle> root );
}
