package de.jbee.silk;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class References {

	public static <T> Dependency<T> type( Type type ) {
		return new TypeReference<T>( type );
	}

	static final class TypeReference<T>
			implements Dependency<T> {

		private final Type type;

		TypeReference( Type type ) {
			super();
			this.type = type;
		}

		@Override
		public ParameterizedType getParameterizedType() {
			return (ParameterizedType) type;
		}

		@Override
		public boolean morePreciseThan( Reference<T> other ) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean fulfills( Dependency<T> dependency ) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int resourceCardinality() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int resourceNr() {
			// TODO Auto-generated method stub
			return 0;
		}

	}
}
