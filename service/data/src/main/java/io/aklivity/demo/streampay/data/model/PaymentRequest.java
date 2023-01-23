/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.demo.streampay.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class PaymentRequest
{
    String fromUserId;
    String toUserId;
    double amount;
    String notes;
    long timestamp;
}