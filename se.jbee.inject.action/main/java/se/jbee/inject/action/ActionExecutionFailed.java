/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.action;

public class ActionExecutionFailed extends RuntimeException {

	public ActionExecutionFailed(String message, Throwable cause) {
		super(message + ": " + cause.getMessage(), cause);
	}

}