package de.jbee.silk;

public class Injectrons<T> {

	private final Injectron<T>[] injectrons;

	Injectrons( Injectron<T>[] injectrons ) {
		super();
		this.injectrons = injectrons;
	}

	public int size() {
		return injectrons.length;
	}

	public Injectron<T> at( int index ) {
		return injectrons[index];
	}

}
