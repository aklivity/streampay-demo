/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.streampay.service;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.aklivity.streampay.data.model.Balance;
import io.aklivity.streampay.data.model.Command;
import io.aklivity.streampay.data.model.PaymentRequest;
import io.aklivity.streampay.data.model.Transaction;
import io.aklivity.streampay.data.serde.SerdeFactory;
import io.aklivity.streampay.service.processor.ProcessTransactionSupplier;
import io.aklivity.streampay.service.processor.ProcessValidCommandSupplier;
import io.aklivity.streampay.service.processor.RejectInvalidCommandSupplier;
import io.aklivity.streampay.service.processor.ValidateCommandSupplier;

@Component
public class PaymentTopology
{
    private final Serde<String> stringSerde = Serdes.String();
    private final Serde<byte[]> bytesSerde = Serdes.ByteArray();
    private final Serde<Command> commandSerde = SerdeFactory.commandSerde();
    private final Serde<PaymentRequest> paymentRequestSerde = SerdeFactory.jsonSerdeFor(PaymentRequest.class, false);
    private final Serde<Transaction> transactionSerde = SerdeFactory.jsonSerdeFor(Transaction.class, false);
    private final Serde<Balance> balanceSerde = SerdeFactory.jsonSerdeFor(Balance.class, false);

    @Value("${commands.topic:commands}")
    String commandsTopic;
    @Value("${command.replies.topic:replies}")
    String commandRepliesTopic;
    @Value("${payment.requests.topic:payment-requests}")
    String paymentRequestsTopic;
    @Value("${balances.topic:balances}")
    String balancesTopic;
    @Value("${transactions.topic:transactions}")
    String transactionsTopic;

    private final String balanceStoreName = "Balance";
    private final String idempotencyKeyStoreName = "IdempotencyKey";
    private final String commandsSource = "CommandsSource";
    private final String transactionsSourceSource = "TransactionSource";
    private final String validateCommand = "ValidateCommand";
    private final String rejectInvalidCommand = "RejectInvalidCommand";
    private final String processValidCommand = "ProcessValidCommand";
    private final String processTransaction = "TransactionProcessor";
    private final String repliesSink = "RepliesSink";
    private final String transactionSink = "TransactionSink";
    private final String paymentRequestSink = "PaymentRequestsSink";
    private final String balancesSink = "BalancesSink";

    public PaymentTopology()
    {
    }

    @Autowired
    public void buildPipeline(
        StreamsBuilder paymentKafkaStreamsBuilder)
    {
        // create store
        final StoreBuilder balanceStoreBuilder = Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(balanceStoreName),
            stringSerde,
            balanceSerde);

        final StoreBuilder idempotencyKeyStoreBuilder = Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(idempotencyKeyStoreName),
            bytesSerde,
            bytesSerde);

        final Topology topologyBuilder = paymentKafkaStreamsBuilder.build();
        topologyBuilder.addSource(commandsSource, stringSerde.deserializer(), commandSerde.deserializer(), commandsTopic)
            .addProcessor(validateCommand, new ValidateCommandSupplier(
                idempotencyKeyStoreName, processValidCommand, rejectInvalidCommand), commandsSource)
            .addProcessor(processValidCommand,
                new ProcessValidCommandSupplier(balanceStoreName, repliesSink, transactionSink, paymentRequestSink),
                validateCommand)
            .addProcessor(rejectInvalidCommand, new RejectInvalidCommandSupplier(repliesSink),
                validateCommand)
            .addSink(repliesSink, commandRepliesTopic, stringSerde.serializer(), stringSerde.serializer(),
                processValidCommand, rejectInvalidCommand)
            .addSink(transactionSink, transactionsTopic, stringSerde.serializer(), transactionSerde.serializer(),
                processValidCommand)
            .addSink(paymentRequestSink, paymentRequestsTopic, stringSerde.serializer(), paymentRequestSerde.serializer(),
                processValidCommand)
            .addSource(transactionsSourceSource, stringSerde.deserializer(), transactionSerde.deserializer(),
                transactionsTopic)
            .addProcessor(processTransaction, new ProcessTransactionSupplier(balanceStoreName, balancesSink),
                transactionsSourceSource)
            .addSink(balancesSink, balancesTopic, stringSerde.serializer(), balanceSerde.serializer(), processTransaction)
            .addStateStore(balanceStoreBuilder, processTransaction, processValidCommand)
            .addStateStore(idempotencyKeyStoreBuilder, validateCommand);

        System.out.println(topologyBuilder.describe());
    }
}
