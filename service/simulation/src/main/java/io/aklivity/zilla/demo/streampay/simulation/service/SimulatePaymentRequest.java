/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.zilla.demo.streampay.simulation.service;

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
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import io.aklivity.zilla.demo.streampay.data.model.PaymentRequest;
import io.aklivity.zilla.demo.streampay.data.model.User;

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

    public void requestPaymentForVirtualUser()
    {
        final PaymentRequest paymentRequest = creatPaymentRequestForVirtualUser();
        if (paymentRequest != null)
        {
            kafkaTemplate.send(paymentRequestsTopic, UUID.randomUUID().toString(), paymentRequest);
        }
    }

    public void requestPaymentForRealUser()
    {
        final PaymentRequest paymentRequest = creatPaymentRequestForRealUser();
        if (paymentRequest != null)
        {
            Message<PaymentRequest> message = MessageBuilder
                .withPayload(paymentRequest)
                .setHeader(KafkaHeaders.TOPIC, paymentRequestsTopic)
                .setHeader(KafkaHeaders.KEY, UUID.randomUUID().toString())
                .setHeader("content-type", "application/json")
                .setHeader("zilla:identity", paymentRequest.getToUserId())
                .build();
            kafkaTemplate.send(message);
        }
    }

    private PaymentRequest creatPaymentRequestForVirtualUser()
    {
        final User fromUser = simulateUser.randomVirtualUser();
        final User toUser = simulateUser.randomVirtualUser();
        final double amount = BigDecimal.valueOf(random.nextDouble(1, 500))
            .setScale(2, RoundingMode.HALF_DOWN).doubleValue();

        PaymentRequest paymentRequest = null;

        if (fromUser != toUser)
        {
            paymentRequest = PaymentRequest.builder()
                .amount(amount)
                .fromUserId(fromUser.getId())
                .fromUserName(fromUser.getName())
                .toUserId(toUser.getId())
                .toUserName(toUser.getName())
                .notes("Please")
                .timestamp(Instant.now().toEpochMilli())
                .build();

            LOGGER.info("Payment Requested from {} to {}", fromUser, toUser);
        }


        return paymentRequest;
    }

    private PaymentRequest creatPaymentRequestForRealUser()
    {
        final User fromUser = simulateUser.randomVirtualUser();
        final User toUser = simulateUser.randomRealUser();
        final double amount = BigDecimal.valueOf(random.nextDouble(1, 500))
            .setScale(2, RoundingMode.HALF_DOWN).doubleValue();

        PaymentRequest paymentRequest = null;

        if (toUser != null)
        {
            paymentRequest = PaymentRequest.builder()
                .amount(amount)
                .fromUserId(fromUser.getId())
                .fromUserName(fromUser.getName())
                .toUserId(toUser.getId())
                .toUserName(toUser.getName())
                .notes("Please send me some money")
                .timestamp(Instant.now().toEpochMilli())
                .build();

            LOGGER.info("Payment Requested from {} to {}", fromUser, toUser.getId());
        }


        return paymentRequest;
    }


}
