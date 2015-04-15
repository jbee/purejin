package se.jbee.inject.service;

public class ServiceMalfunction extends RuntimeException {

	public ServiceMalfunction(String message, Throwable cause) {
		super(message, cause);
	}
	
}