package com.maintena.exception;

/**
 * Conflicto con el estado actual del recurso (por ejemplo, borrar algo que
 * todavía tiene dependencias). Se traduce a HTTP 409.
 */
public class ConflictException extends RuntimeException {
	public ConflictException(String message) {
		super(message);
	}
}
