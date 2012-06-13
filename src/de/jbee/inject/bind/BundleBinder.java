package de.jbee.inject.bind;

public interface BundleBinder {

	Binding<?>[] install( Class<? extends Bundle> root );
}
