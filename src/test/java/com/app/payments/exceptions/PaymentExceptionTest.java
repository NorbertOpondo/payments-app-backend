package com.app.payments.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentExceptionTest {

    @Test
    void notFound_setsCorrectStatusAndMessage() {
        PaymentException ex = PaymentException.notFound("Transaction not found: abc");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("Transaction not found: abc");
    }

    @Test
    void badRequest_setsCorrectStatusAndMessage() {
        PaymentException ex = PaymentException.badRequest("Invalid phone number");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("Invalid phone number");
    }

    @Test
    void internalError_setsCorrectStatusAndMessage() {
        PaymentException ex = PaymentException.internalError("Gateway unreachable");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(ex.getMessage()).isEqualTo("Gateway unreachable");
    }

    @Test
    void directConstructor_acceptsArbitraryStatus() {
        PaymentException ex = new PaymentException("Conflict detected", HttpStatus.CONFLICT);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("Conflict detected");
    }

    @Test
    void isRuntimeException() {
        assertThat(PaymentException.notFound("test")).isInstanceOf(RuntimeException.class);
    }
}
