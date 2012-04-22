package de.jbee.silk;

import de.jbee.silk.Suppliers.ProviderSupplier;

public interface Binder {

	<T> TypedBinder<T> bind( Instance<T> instance );

	//<T> TypedArrayBinder<T> bind( Class<T[]> type );

	//void install( Module module ); // this would allow doing narrowed installations - could be confusing

	interface TypedBinder<T> {

		void to( Supplier<? extends T> supplier );

	}

	interface RootBinder
			extends ScopedBinder {

		ScopedBinder in( Scope scope );
	}

	/**
	 * A {@link Scope} had been defined.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	interface ScopedBinder
			extends TargetedBinder {

		// means when the type/instance is created and dependencies are injected into it
		TargetedBinder injectingInto( Class<?> scope ); //move to a util method

		TargetedBinder injectingInto( Instance<?> scope );
	}

	/**
	 * Bindings have been restricted and are just effective within a special scope defined before
	 * using {@link ScopedBinder#injectingInto(Class)}-clauses.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	interface TargetedBinder
			extends LocalisedBinder {

		//TODO improve this since from a dependency point of view it is good to localize all binds somehow
		// instead of narrow explicit we could expose explicit and make binds as narrow as possible by default (classic interface to impl binds in same package)

		LocalisedBinder inPackageOf( Class<?> packageOf );

		LocalisedBinder belowPackageOf( Class<?> packageOf );

		LocalisedBinder beneathPackageOf( Class<?> packageOf );
	}

	interface TypedArrayBinder<T> {

		void to( Class<? extends T> src1 );

		void to( Class<? extends T> src1, Class<? extends T> src2 );

		void to( Class<? extends T> src1, Class<? extends T> src2, Class<? extends T> src3 );

		// and so on.... will avoid the warning here 
	}

	interface LocalisedBinder
			extends Binder {

		LocalisedBinder havingParent( Class<?> type );

		LocalisedBinder havingDirectParent( Class<?> type );

	}

	class Test {

		public static void test( RootBinder root ) {
			Binder binder = root.in( Scoped.APPLICATION ).injectingInto( String.class ).inPackageOf(
					Instance.class );

			CoreBinder binder2 = new CoreBinder();
			//binder.bind( Number[].class ).to( Integer.class, Double.class ); // a simple way to receive 2 impl. for a array of their supertype
			binder2.bind( Provider.class ).to( new ProviderSupplier() );
			binder2.bind( Provider.class ).toSupplier( ProviderSupplier.class );
			binder2.bind( Number.class ).to( Integer.class );

			// the binds made with this binder will just be effective WHEN types are injected into Repository that is a member in or injected into a Context object that again is a member or injected into a Module.
			LocalisedBinder scopedBinder = root.in( Scoped.THREAD ).injectingInto( Repository.class ).havingParent(
					ResourceContext.class ).havingParent( Module.class );
		}
	}

}
