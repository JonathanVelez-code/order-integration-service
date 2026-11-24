package com.jonathanvelez.orderintegration.external;

/** A temporary CRM failure, such as a timeout. Worth retrying. */
public class CrmUnavailableException extends RuntimeException {

    public CrmUnavailableException(String message) {
        super(message);
    }
}
