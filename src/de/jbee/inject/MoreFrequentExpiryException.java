package de.jbee.inject;

public class MoreFrequentExpiryException
		extends RuntimeException {

	public MoreFrequentExpiryException( Injection parent, Injection injection ) {
		super( "Cannot inject " + injection.getTarget() + " into " + parent.getTarget() );
	}

	@Override
	public String toString() {
		return getMessage();
	}
}
