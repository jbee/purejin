/**
 * 
 */
package se.jbee.inject.bind;

public interface LocalisedBasicBinder
		extends BasicBinder {

	LocalisedBasicBinder havingParent( Class<?> type );

	LocalisedBasicBinder havingDirectParent( Class<?> type );

}