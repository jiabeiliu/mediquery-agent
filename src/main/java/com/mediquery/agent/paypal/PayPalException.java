package com.mediquery.agent.paypal;

/**
 * Thrown when a PayPal API call fails. Carries the HTTP status when known.
 */
public class PayPalException extends RuntimeException {

    private final Integer httpStatus;

    public PayPalException(String message) {
        super(message);
        this.httpStatus = null;
    }

    public PayPalException(String message, Integer httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public PayPalException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = null;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }
}
