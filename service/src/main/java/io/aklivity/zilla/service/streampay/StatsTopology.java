/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.zilla.service.streampay;

import java.time.Instant;
import java.util.Map;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.aklivity.zilla.service.streampay.model.Event;
import io.aklivity.zilla.service.streampay.model.PaymentRequest;
import io.aklivity.zilla.service.streampay.model.Transaction;
import io.aklivity.zilla.service.streampay.model.User;
import io.aklivity.zilla.service.streampay.serde.SerdeFactory;

@Component
public class StatsTopology
{
    private final Serde<String> stringSerde = Serdes.String();
    private final Serde<PaymentRequest> paymentRequestSerde = SerdeFactory.jsonSerdeFor(PaymentRequest.class, false);
    private final Serde<Transaction> transactionSerde = SerdeFactory.jsonSerdeFor(Transaction.class, false);
    private final Serde<Event> eventSerde = SerdeFactory.jsonSerdeFor(Event.class, false);
    private final Serde<User> userSerde = SerdeFactory.jsonSerdeFor(User.class, false);

    @Value("${payment.requests.topic:payment-requests}")
    String paymentRequestsTopic;

    @Value("${transactions.topic:transactions}")
    String transactionsTopic;

    @Value("${activities.topic:activities}")
    String activitiesTopic;

    @Value("${users.topic:users}")
    String usersTopic;

    public StatsTopology()
    {
    }

    @Autowired
    public void buildPipeline(StreamsBuilder satsKafkaStreamsBuilder)
    {
        KTable<String, User> users = satsKafkaStreamsBuilder.table(usersTopic,
            Consumed.with(stringSerde, userSerde), Materialized.with(stringSerde, userSerde));

        final String paymentSent = "PaymentSent";
        final String paymentReceived = "PaymentReceived";
        String branch = "Branch-";
        final Map<String, KStream<String, Transaction>> transactionBranches = satsKafkaStreamsBuilder.stream(transactionsTopic,
                Consumed.with(stringSerde, transactionSerde))
            .split(Named.as(branch))
            .branch((key, value) -> value.getAmount() < 0, Branched.as(paymentSent))
            .branch((key, value) -> value.getAmount() >= 0, Branched.as(paymentReceived))
            .defaultBranch();

        transactionBranches.get(branch + paymentSent)
            .toTable(Named.as(paymentSent), Materialized.with(stringSerde, transactionSerde))
            .leftJoin(users, Transaction::getUserId, (tran, user) ->
                Event.builder()
                .eventName(paymentSent)
                .amount(tran.getAmount())
                .timestamp(Instant.now().toEpochMilli())
                .toUserId(tran.getUserId())
                .toUserName(user.getName())
                .fromUserId(tran.getOwnerId())
                .build(), Materialized.with(stringSerde, eventSerde))
            .leftJoin(users, Event::getFromUserId, (event, user) ->
            {
                event.setFromUserName(user.getName());
                return event;
            }, Materialized.with(stringSerde, eventSerde))
            .toStream()
            .to(activitiesTopic, Produced.with(stringSerde, eventSerde));

        transactionBranches.get(branch + paymentReceived)
            .toTable(Named.as(paymentReceived), Materialized.with(stringSerde, transactionSerde))
            .leftJoin(users, Transaction::getUserId, (tran, user) ->
                Event.builder()
                    .eventName(paymentReceived)
                    .amount(tran.getAmount())
                    .timestamp(Instant.now().toEpochMilli())
                    .fromUserId(tran.getUserId())
                    .fromUserName(user.getName())
                    .toUserId(tran.getOwnerId())
                    .build(), Materialized.with(stringSerde, eventSerde))
            .leftJoin(users, Event::getToUserId, (event, user) ->
            {
                event.setToUserName(user.getName());
                return event;
            }, Materialized.with(stringSerde, eventSerde))
            .toStream()
            .to(activitiesTopic, Produced.with(stringSerde, eventSerde));

        satsKafkaStreamsBuilder.table(paymentRequestsTopic,
                Consumed.with(stringSerde, paymentRequestSerde), Materialized.with(stringSerde, paymentRequestSerde))
            .filter((key, value) -> value != null)
            .leftJoin(users, PaymentRequest::getToUserId, (req, user) ->
                Event.builder()
                    .eventName("PaymentRequested")
                    .amount(req.getAmount())
                    .timestamp(Instant.now().toEpochMilli())
                    .toUserId(req.getToUserId())
                    .toUserName(user.getName())
                    .fromUserId(req.getFromUserId())
                    .build(), Materialized.with(stringSerde, eventSerde))
            .join(users, Event::getFromUserId, (event, user) ->
            {
                event.setFromUserName(user.getName());
                return event;
            }, Materialized.with(stringSerde, eventSerde))
            .toStream()
            .to(activitiesTopic);
    }
}
