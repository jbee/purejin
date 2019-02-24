/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.action;

public class ActionMalfunction extends RuntimeException {

	public ActionMalfunction(String message, Throwable cause) {
		super(message + ": " + cause.getMessage(), cause);
	}

}