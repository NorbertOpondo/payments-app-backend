package com.app.payments.dto;

import com.app.payments.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1.00")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{6,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
