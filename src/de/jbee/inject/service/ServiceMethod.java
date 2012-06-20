package de.jbee.inject.service;

/**
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <R>
 *            The return type of the method wired to this service
 */
public interface ServiceMethod<P, R> {

	R invoke( P params );

	// OPEN how to do classical service-wrapper like logger etc.

	// man kann eigentlich beliebige services auf den DI-service mappen lassen. also auch mit mehreren arg. - diese kÃ¶nnen in ein Object[] verpackt und fÃ¼r den methoden-aufruf wieder entpackt werden.
	// solange man ein adapter hat zwischen DI-Service dem Interface, welches als Service dient und die Typen eindeutig auf eine Impl. passen kann man dazwischen packen und entpacken
	// wenn es ein allg. service interface ist und man den parameteransatz verwendet muss man auch nicht eine impl fÃ¼r das eigene Service-Interface angeben
	// allerdings: die ServiceFactory muss aus dem DI-Service die benÃ¶tigte Klasse machen. Das geht nur einfach, wenn es ein gemeinsames Interface gibt. Sonst muss fÃ¼r jedes ein Adapter geschrieben werden oder generiert mit CGLib oder Ã¤hnlichem - unschÃ¶n

}
