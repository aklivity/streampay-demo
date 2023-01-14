/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.zilla.service.streampay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

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

import io.aklivity.zilla.service.streampay.model.Balance;
import io.aklivity.zilla.service.streampay.model.Command;
import io.aklivity.zilla.service.streampay.model.PayCommand;
import io.aklivity.zilla.service.streampay.model.Transaction;
import io.confluent.kafka.serializers.KafkaJsonDeserializer;

public class PaymentTopologyTest
{
    private static final String COMMANDS_TOPIC = "commands";
    private static final String COMMANDS_REPLIES_TOPIC = "replies";
    private static final String PAYMENT_REQUESTS_TOPIC = "payment-requests";
    private static final String TRANSACTIONS_TOPIC = "transactions";
    private static final String BALANCES_TOPIC = "balances";

    private TopologyTestDriver testDriver;

    private TestInputTopic<String, Command> commandsInTopic;
    private TestInputTopic<String, Transaction> transactionsInTopic;
    private TestOutputTopic<String, Balance> balancesOutTopic;

    @BeforeEach
    public void setUp()
    {
        final StreamsBuilder builder = new StreamsBuilder();
        final PaymentTopology stream = new PaymentTopology();
        stream.commandsTopic = COMMANDS_TOPIC;
        stream.commandRepliesTopic = COMMANDS_REPLIES_TOPIC;
        stream.paymentRequestsTopic = PAYMENT_REQUESTS_TOPIC;
        stream.balancesTopic = BALANCES_TOPIC;
        stream.transactionsTopic = TRANSACTIONS_TOPIC;
        stream.buildPipeline(builder);
        final Topology topology = builder.build();

        final Properties props = new Properties();
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName());
        testDriver = new TopologyTestDriver(topology, props);

        transactionsInTopic = testDriver.createInputTopic(TRANSACTIONS_TOPIC,
                new StringSerializer(), new JsonSerializer<>());
        commandsInTopic = testDriver.createInputTopic(COMMANDS_TOPIC,
            new StringSerializer(), new JsonSerializer<>());

        StringDeserializer keyDeserializer = new StringDeserializer();
        KafkaJsonDeserializer<Balance> balanceDeserializer = new KafkaJsonDeserializer<>();
        balanceDeserializer.configure(Collections.emptyMap(), false);
        balancesOutTopic = testDriver.createOutputTopic(BALANCES_TOPIC,
            keyDeserializer, balanceDeserializer);
    }

    @AfterEach
    public void tearDown()
    {
        testDriver.close();
    }

    @Test
    public void shouldProcessPayCommand()
    {
        final Headers headers = new RecordHeaders(
            new Header[]{
                new RecordHeader("zilla:domain-model", "PayCommand".getBytes()),
                new RecordHeader("user-id", "user1".getBytes()),
                new RecordHeader("zilla:correlation-id", "1".getBytes()),
                new RecordHeader("idempotency-key", "pay1".getBytes()),
                new RecordHeader(":path", "/pay".getBytes())
            });

        transactionsInTopic.pipeInput(new TestRecord<>("user1", Transaction.builder()
            .amount(123)
            .date(Date.from(Instant.now()))
            .build()));

        commandsInTopic.pipeInput(new TestRecord<>("pay1", PayCommand.builder()
            .userId("user2")
            .amount(123)
            .build(), headers));
        List<KeyValue<String, Balance>> balances = balancesOutTopic.readKeyValuesToList();
        assertEquals(3, balances.size());
    }

    @Test
    public void shouldProcessRequestCommand()
    {
        final Headers headers = new RecordHeaders(
            new Header[]{
                new RecordHeader("zilla:domain-model", "RequestCommand".getBytes()),
                new RecordHeader("user-id", "user1".getBytes()),
                new RecordHeader("zilla:correlation-id", "1".getBytes()),
                new RecordHeader("idempotency-key", "pay1".getBytes()),
                new RecordHeader(":path", "/pay".getBytes())
            });

        commandsInTopic.pipeInput(new TestRecord<>("pay1", PayCommand.builder()
            .userId("user2")
            .amount(123)
            .notes("test")
            .build(), headers));
    }
}
