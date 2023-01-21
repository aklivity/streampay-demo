/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.streampay.simulation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import io.aklivity.streampay.data.model.PaymentRequest;

@Service
public class SimulatePaymentRequest
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatePaymentRequest.class);

    @Value("${payment.requests.topic:payment-requests}")
    String paymentRequestsTopic;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private SimulateUser simulateUser;

    @Autowired
    private Random random;

    public void requestPayment()
    {
        final PaymentRequest paymentRequest = creatPaymentRequest();
        if (paymentRequest != null)
        {
            kafkaTemplate.send(paymentRequestsTopic, UUID.randomUUID().toString(), paymentRequest);
        }
    }

    private PaymentRequest creatPaymentRequest()
    {
        final int fromUserId = simulateUser.randomUserId();
        final int toUserId = simulateUser.randomUserId();
        final double amount = new BigDecimal(random.nextDouble(1, 500))
            .setScale(2, RoundingMode.HALF_DOWN).doubleValue();
        assert fromUserId != toUserId;

        PaymentRequest paymentRequest = null;

        if (fromUserId != toUserId)
        {
            paymentRequest = PaymentRequest.builder()
                .amount(amount)
                .fromUserId(String.format("virtual-user-%d", fromUserId))
                .toUserId(String.format("virtual-user-%d", toUserId))
                .notes("Please")
                .timestamp(Instant.now().toEpochMilli())
                .build();

            LOGGER.info("Payment Requested from {} to {}", fromUserId, toUserId);
        }


        return paymentRequest;
    }


}
