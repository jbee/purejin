package de.jbee.silk.draft;

public interface ServiceBinderExample {

	interface Binder {

		<T> ServiceBinder<T> bindService( Class<T> service );

		// damit services ein add-on character bekommen kÃ¶nnen braucht man eine mÃ¶glichkeit
		// bindings zu markeiren - und alle markierten in einem supplier abzufragen

		// vielleicht kann man auch listener anmelden auf die im folgenden durchgefÃ¼hrten binds
		// dann kann man ein ServiceListener schreiben der dann alle klasse mitbekommt und die service-methoden schonmal raussuchen kann
		// ---> Es ist mehr eine init-phase wenn alle binds gemacht sind. Listener ist das falsche wort
		//		die init-teilnehmer bekommen die gemachten binds und kÃ¶nnen diese so auswerten und vielleicht markieren oder sowas
		//		mit einer markierung kÃ¶nnten die klasse mit den Service-methoden wieder entbunden werden
		// die erweiterung hat dass dann ganz unter kontrolle
		// ---> dann wÃ¤re es sogar sinnvoll binds zu machen, die quasi nirgends gÃ¼ltig sind (fÃ¼r inject)

		// und dann etwas um Service-Wrapper anzumelden wie etwa logger etc.

		// man kann eigentlich beliebige services auf den DI-service mappen lassen. also auch mit mehreren arg. - diese kÃ¶nnen in ein Object[] verpackt und fÃ¼r den methoden-aufruf wieder entpackt werden.
		// solange man ein adapter hat zwischen DI-Service dem Interface, welches als Service dient und die Typen eindeutig auf eine Impl. passen kann man dazwischen packen und entpacken
		// wenn es ein allg. service interface ist und man den parameteransatz verwendet muss man auch nicht eine impl fÃ¼r das eigene Service-Interface angeben
		// allerdings: die ServiceFactory muss aus dem DI-Service die benÃ¶tigte Klasse machen. Das geht nur einfach, wenn es ein gemeinsames Interface gibt. Sonst muss fÃ¼r jedes ein Adapter geschrieben werden oder generiert mit CGLib oder Ã¤hnlichem - unschÃ¶n
	}

	interface Binding {

	}

	interface BindingPreparer {

		void prepareFor( Binding... bindings );
	}

	interface ServiceBinder<T> { // could extend BindingPreparer

		void to( ServiceFactory<? extends T> factory );
	}

	interface ServiceFactory<S> {

		<R, T> S wrap( Service<R, T> service, Class<R> returnType, Class<T> parameterType );
	}

	interface Service<R, T> {

	}

	class MyService<R, T> {

	}

	class MyServiceFactory
			implements ServiceFactory<MyService<?, ?>> {

		@Override
		public <R, T> MyService<R, T> wrap( Service<R, T> service, Class<R> returnType,
				Class<T> parameterType ) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	class Test {

		public void test( Binder binder ) {
			binder.bindService( MyService.class ).to( new MyServiceFactory() );
		}
	}

}
