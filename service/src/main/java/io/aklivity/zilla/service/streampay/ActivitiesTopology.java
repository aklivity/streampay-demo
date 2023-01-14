/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.zilla.service.streampay;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.aklivity.zilla.service.streampay.model.Event;
import io.aklivity.zilla.service.streampay.model.PaymentRequest;
import io.aklivity.zilla.service.streampay.model.Transaction;
import io.aklivity.zilla.service.streampay.serde.SerdeFactory;

@Component
public class ActivitiesTopology
{
    private final Serde<String> stringSerde = Serdes.String();
    private final Serde<PaymentRequest> paymentRequestSerde = SerdeFactory.jsonSerdeFor(PaymentRequest.class, false);
    private final Serde<Transaction> transactionSerde = SerdeFactory.jsonSerdeFor(Transaction.class, false);
    private final Serde<Event> eventSerde = SerdeFactory.jsonSerdeFor(Event.class, false);


    @Value("${payment.requests.topic:payment-requests}")
    String paymentRequestsTopic;

    @Value("${transactions.topic:transactions}")
    String transactionsTopic;

    @Value("${activities.topic:activities}")
    String activitiesTopic;

    public ActivitiesTopology()
    {
    }

    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder)
    {
        streamsBuilder.stream(transactionsTopic,
            Consumed.with(stringSerde, transactionSerde))
            .map((key, value) ->
                new KeyValue<>(key, Event.builder()
                    .eventName(value.getAmount() < 0 ? "PaymentSent" : "PaymentReceived")
                    .amount(value.getAmount())
                    .date(value.getDate())
                    .userId(value.getUserId())
                    .build()))
            .to(activitiesTopic, Produced.with(stringSerde, eventSerde));

        streamsBuilder.stream(paymentRequestsTopic,
                Consumed.with(stringSerde, paymentRequestSerde))
            .map((key, value) ->
                new KeyValue<>(key, Event.builder()
                    .eventName("PaymentRequested")
                    .amount(value.getAmount())
                    .date(value.getDate())
                    .userId(value.getUserId())
                    .date(value.getDate())
                    .build()))
            .to(activitiesTopic, Produced.with(stringSerde, eventSerde));
    }
}
