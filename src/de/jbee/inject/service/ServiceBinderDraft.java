package de.jbee.inject.service;

import de.jbee.inject.Type;

public interface ServiceBinderDraft {

	interface Binder {

		<T> ServiceBinder<T> bindService( Class<T> service );

		// OPEN etwas um Service-Wrapper anzumelden wie etwa logger etc.

		// man kann eigentlich beliebige services auf den DI-service mappen lassen. also auch mit mehreren arg. - diese kÃ¶nnen in ein Object[] verpackt und fÃ¼r den methoden-aufruf wieder entpackt werden.
		// solange man ein adapter hat zwischen DI-Service dem Interface, welches als Service dient und die Typen eindeutig auf eine Impl. passen kann man dazwischen packen und entpacken
		// wenn es ein allg. service interface ist und man den parameteransatz verwendet muss man auch nicht eine impl fÃ¼r das eigene Service-Interface angeben
		// allerdings: die ServiceFactory muss aus dem DI-Service die benÃ¶tigte Klasse machen. Das geht nur einfach, wenn es ein gemeinsames Interface gibt. Sonst muss fÃ¼r jedes ein Adapter geschrieben werden oder generiert mit CGLib oder Ã¤hnlichem - unschÃ¶n
	}

	interface ServiceBinder<T> { // could extend BindingPreparer

		void to( ServiceFactory<? extends T> factory );
	}

	interface ServiceFactory<S> {

		/**
		 * Wraps the given service in a object S that is a 'service' as well but the class is not
		 * part of the DI framework. Therbey it is possible to use a won service-interface but also
		 * make use of all the build in functionality operation of the {@link Service}-interface.
		 */
		<R, T> S decouple( Service<R, T> service, Type<R> returnType, Type<T> parameterType );
	}

	interface Service<R, T> {

	}

	class MyService<R, T> {

	}

	class MyServiceFactory
			implements ServiceFactory<MyService<?, ?>> {

		@Override
		public <R, T> MyService<R, T> decouple( Service<R, T> service, Type<R> returnType,
				Type<T> parameterType ) {
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
