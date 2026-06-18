package com.app.payments.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PaymentException extends RuntimeException {

    private final HttpStatus status;

    public PaymentException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public static PaymentException notFound(String message) {
        return new PaymentException(message, HttpStatus.NOT_FOUND);
    }

    public static PaymentException badRequest(String message) {
        return new PaymentException(message, HttpStatus.BAD_REQUEST);
    }

    public static PaymentException internalError(String message) {
        return new PaymentException(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
