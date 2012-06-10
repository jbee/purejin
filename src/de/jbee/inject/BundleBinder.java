package de.jbee.inject;

public interface BundleBinder {

	Binding<?>[] install( Class<? extends Bundle> root );
}
