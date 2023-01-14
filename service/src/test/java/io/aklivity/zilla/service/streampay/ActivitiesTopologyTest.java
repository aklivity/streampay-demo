/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.zilla.service.streampay;

import io.aklivity.zilla.service.streampay.model.Balance;
import io.aklivity.zilla.service.streampay.model.Command;
import io.aklivity.zilla.service.streampay.model.PayCommand;
import io.aklivity.zilla.service.streampay.model.PaymentRequest;
import io.aklivity.zilla.service.streampay.model.Transaction;
import io.confluent.kafka.serializers.KafkaJsonDeserializer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActivitiesTopologyTest
{
    private static final String PAYMENT_REQUESTS_TOPIC = "payment-requests";
    private static final String TRANSACTIONS_TOPIC = "transactions";
    private static final String ACTIVITIES_TOPIC = "activities";

    private TopologyTestDriver testDriver;

    private TestInputTopic<String, Transaction> transactionsInTopic;
    private TestInputTopic<String, PaymentRequest> paymentRequestsInTopic;

    @BeforeEach
    public void setUp()
    {
        final StreamsBuilder builder = new StreamsBuilder();
        final ActivitiesTopology stream = new ActivitiesTopology();
        stream.paymentRequestsTopic = PAYMENT_REQUESTS_TOPIC;
        stream.transactionsTopic = TRANSACTIONS_TOPIC;
        stream.activitiesTopic = ACTIVITIES_TOPIC;
        stream.buildPipeline(builder);
        final Topology topology = builder.build();

        final Properties props = new Properties();
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName());
        testDriver = new TopologyTestDriver(topology, props);

        transactionsInTopic = testDriver.createInputTopic(TRANSACTIONS_TOPIC,
                new StringSerializer(), new JsonSerializer<>());

        paymentRequestsInTopic = testDriver.createInputTopic(PAYMENT_REQUESTS_TOPIC,
            new StringSerializer(), new JsonSerializer<>());
    }

    @AfterEach
    public void tearDown()
    {
        testDriver.close();
    }

    @Test
    public void shouldProcessTransaction()
    {
        transactionsInTopic.pipeInput(new TestRecord<>("user1", Transaction.builder()
            .amount(123)
            .userId("user2")
            .date(Date.from(Instant.now()))
            .build()));
    }

    @Test
    public void shouldProcessPaymentRequest()
    {
        paymentRequestsInTopic.pipeInput(new TestRecord<>("user1", PaymentRequest.builder()
            .amount(123)
            .userId("user2")
            .date(Date.from(Instant.now()))
            .build()));
    }
}
