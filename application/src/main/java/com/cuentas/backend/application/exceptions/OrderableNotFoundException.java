package com.cuentas.backend.application.exceptions;


public class OrderableNotFoundException extends RuntimeException {

	public OrderableNotFoundException(String message, String errorCode) {

		super(message);

	}

}
