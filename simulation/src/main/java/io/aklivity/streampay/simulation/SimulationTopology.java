/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.streampay.simulation;

import java.time.Instant;
import java.util.UUID;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.aklivity.streampay.data.model.PaymentRequest;
import io.aklivity.streampay.data.model.Transaction;
import io.aklivity.streampay.data.model.User;
import io.aklivity.streampay.data.serde.SerdeFactory;

@Component
public class SimulationTopology
{
    private final Serde<String> stringSerde = Serdes.String();
    private final Serde<PaymentRequest> paymentRequestSerde = SerdeFactory.jsonSerdeFor(PaymentRequest.class, false);
    private final Serde<Transaction> transactionSerde = SerdeFactory.jsonSerdeFor(Transaction.class, false);
    private final Serde<User> userSerde = SerdeFactory.jsonSerdeFor(User.class, false);

    @Value("${payment.requests.topic:payment-requests}")
    String paymentRequestsTopic;

    @Value("${transactions.topic:transactions}")
    String transactionsTopic;

    @Value("${users.topic:users}")
    String usersTopic;

    public SimulationTopology()
    {
    }

    @Autowired
    public void buildPipeline(
        StreamsBuilder simulationKafkaStreamsBuilder)
    {
        GlobalKTable<String, User> users = simulationKafkaStreamsBuilder.globalTable(usersTopic,
            Consumed.with(stringSerde, userSerde), Materialized.with(stringSerde, userSerde));

        simulationKafkaStreamsBuilder.stream(paymentRequestsTopic, Consumed.with(stringSerde, paymentRequestSerde))
            .filter((key, value) -> value != null && value.getFromUserId().startsWith("virtual-user"))
            .leftJoin(users, (id, r) -> r.getToUserId(), (req, user) ->
                Transaction.builder()
                    .id(UUID.randomUUID())
                    .ownerId(req.getFromUserId())
                    .userId(req.getToUserId())
                    .amount(req.getAmount())
                    .timestamp(Instant.now().toEpochMilli())
                    .build()
            )
            .map((key, value) -> new KeyValue(value.getOwnerId(), value))
            .to(transactionsTopic, Produced.with(stringSerde, transactionSerde));
        System.out.println(simulationKafkaStreamsBuilder.build().describe());
    }
}
