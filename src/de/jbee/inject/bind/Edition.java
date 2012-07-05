package de.jbee.inject.bind;

public interface Edition {

	Edition FULL = new Edition() {

		@Override
		public boolean featured( Class<?> bundleOrModule ) {
			return true;
		}
	};

	boolean featured( Class<?> bundleOrModule );
}
