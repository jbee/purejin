package de.jbee.silk.builder;

import de.jbee.silk.Instance;
import de.jbee.silk.Scope;
import de.jbee.silk.Supplier;

public interface PresetBinder {

	<T> TypedBinder<T> bind( Instance<T> instance );

	//<T> TypedArrayBinder<T> bind( Class<T[]> type );

	//void install( Module module ); // this would allow doing narrowed installations - could be confusing

	interface TypedBinder<T> {

		void to( Supplier<? extends T> supplier );

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

	interface LocalisedBinder
			extends PresetBinder {

		LocalisedBinder havingParent( Class<?> type );

		LocalisedBinder havingDirectParent( Class<?> type );

	}

	/**
	 * @deprecated No further interface is needed - we can do this with covariant return type
	 *             overrides only.
	 */
	@Deprecated
	interface TypedMultiBinder<T> {

		void to( Class<? extends T> src1 );

		void to( Class<? extends T> src1, Class<? extends T> src2 );

		void to( Class<? extends T> src1, Class<? extends T> src2, Class<? extends T> src3 );

		// and so on.... will avoid the warning here 
	}
}
