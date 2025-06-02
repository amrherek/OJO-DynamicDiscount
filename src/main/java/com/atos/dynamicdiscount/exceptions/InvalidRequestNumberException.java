package com.atos.dynamicdiscount.exceptions;

/**
 * Thrown when a bill-cycle code is invalid or cannot be used.
 */
public class InvalidRequestNumberException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     * @param message the detail message
     */
    public InvalidRequestNumberException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public InvalidRequestNumberException(String message, Throwable cause) {
        super(message, cause);
    }
}
