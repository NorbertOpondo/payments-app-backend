package com.app.payments.integrations;

import com.app.payments.integrations.dto.GatewayRequest;
import com.app.payments.integrations.dto.GatewayResponse;

public interface PaymentGatewayClient {
    GatewayResponse processPayment(GatewayRequest request);
}
