package io.aklivity.zilla.service.streampay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Transaction
{
    UUID id;
    double amount;
    long timestamp;
}
