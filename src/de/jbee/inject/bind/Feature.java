package de.jbee.inject.bind;

public interface Feature<T extends Enum<T>> {

	T featureOf( Class<?> bundleOrModule );
}
