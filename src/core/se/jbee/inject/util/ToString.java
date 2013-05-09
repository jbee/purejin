package se.jbee.inject.util;

import java.util.Arrays;

public final class ToString {

	public static String describe( Object behaviour ) {
		return "<" + behaviour + ">";
	}

	public static String describe( Object behaviour, Object variant ) {
		return "<" + behaviour + ":" + variant + ">";
	}

	public static String describe( Object behaviour, Object[] variants ) {
		return describe( behaviour, Arrays.toString( variants ) );
	}
}
