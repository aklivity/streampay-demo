/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.streampay.simulation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.aklivity.streampay.simulation.service.SimulatePayment;
import io.aklivity.streampay.simulation.service.SimulatePaymentRequest;
import io.aklivity.streampay.simulation.service.SimulateUser;

@Component
public class SimulationTask
{
    @Autowired
    private SimulateUser simulateUser;

    @Autowired
    private SimulatePaymentRequest simulatePaymentRequest;

    @Autowired
    private SimulatePayment simulatePayment;

    @Scheduled(fixedRateString = "${user.creation.rate:5000}")
    public void scheduleVirtualUserCreation()
    {
        simulateUser.createUser();
    }

    @Scheduled(fixedRateString = "${payment.request.rate:12000}")
    public void schedulePaymentRequest()
    {
        simulatePaymentRequest.requestPayment();
    }

    @Scheduled(fixedRateString = "${payment.rate:10000}")
    public void schedulePayment()
    {
        simulatePayment.makePayment();
    }
}
