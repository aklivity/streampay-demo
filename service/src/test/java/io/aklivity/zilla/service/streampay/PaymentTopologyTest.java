/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.zilla.service.streampay;

import java.util.Properties;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.kafka.support.serializer.JsonSerializer;

import io.aklivity.zilla.service.streampay.model.Command;
import io.aklivity.zilla.service.streampay.model.PayCommand;

public class PaymentTopologyTest
{
    private static final String COMMANDS_TOPIC = "commands";
    private static final String COMMANDS_REPLIES_TOPIC = "replies";
    private static final String PAYMENT_REQUESTS_TOPIC = "payment-requests";
    private static final String TRANSACTIONS_TOPIC = "transactions";
    private static final String BALANCES_TOPIC = "balances";

    private TopologyTestDriver testDriver;

    private TestInputTopic<String, Command> commandsInTopic;

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

        commandsInTopic = testDriver.createInputTopic(COMMANDS_TOPIC,
                new StringSerializer(), new JsonSerializer<>());
    }

    @AfterEach
    public void tearDown()
    {
        testDriver.close();
    }

    @Test
    public void shouldProcessCreateTaskCommand()
    {
        final Headers headers = new RecordHeaders(
            new Header[]{
                new RecordHeader("zilla:domain-model", "PayCommand".getBytes()),
                new RecordHeader("user-id", "user1".getBytes()),
                new RecordHeader("zilla:correlation-id", "1".getBytes()),
                new RecordHeader("idempotency-key", "pay1".getBytes()),
                new RecordHeader(":path", "/pay".getBytes())
            });

        commandsInTopic.pipeInput(new TestRecord<>("pay1", PayCommand.builder()
            .userId("user2")
            .amount(123)
            .build(), headers));
    }
}
