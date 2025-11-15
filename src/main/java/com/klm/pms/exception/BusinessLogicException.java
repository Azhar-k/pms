package com.klm.pms.exception;

/**
 * Exception thrown when a business rule is violated.
 */
public class BusinessLogicException extends RuntimeException {

    public BusinessLogicException(String message) {
        super(message);
    }

    public BusinessLogicException(String message, Throwable cause) {
        super(message, cause);
    }
}

