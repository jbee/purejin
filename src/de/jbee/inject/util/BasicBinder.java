package de.jbee.inject.util;

import de.jbee.inject.Instance;
import de.jbee.inject.Scope;
import de.jbee.inject.Supplier;

public interface BasicBinder {

	<T> TypedBinder<T> bind( Instance<T> instance );

	//void install( Module module ); // this would allow doing narrowed installations - could be confusing

	interface TypedBinder<T> {

		void to( Supplier<? extends T> supplier );

	}

	/**
	 * The ROOT- {@link RootBinder}.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	public interface RootBinder
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

		TargetedBinder injectingInto( Instance<?> target );
	}

	/**
	 * Bindings have been restricted and are just effective within a special scope defined before
	 * using {@link ScopedBinder#injectingInto(Class)}-clauses.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	interface TargetedBinder
			extends BasicBinder /* LocalisedBinder */{

	}

	interface LocalisedBinder
			extends BasicBinder {

		LocalisedBinder havingParent( Class<?> type );

		LocalisedBinder havingDirectParent( Class<?> type );

	}

	//<T> TypedArrayBinder<T> bind( Class<T[]> type );

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
