package de.jbee.inject;

public interface ModuleBinder {

	Binding<?>[] bind( Module root );
}
