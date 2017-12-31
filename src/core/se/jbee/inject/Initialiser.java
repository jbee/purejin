package se.jbee.inject;

/**
 * User can bind a implementation for this interface. The {@link Injector} will
 * resolve all of them and call their {@link #init(Injector)} method as soon as
 * the context is done.
 * 
 * This gives users the possibility to run initialization code once and build
 * more powerful mechanisms on top of it. 
 * 
 * @author jan
 */
@FunctionalInterface
public interface Initialiser {

	void init(Injector context);
}
