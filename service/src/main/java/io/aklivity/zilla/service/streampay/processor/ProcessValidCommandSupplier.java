package io.aklivity.zilla.service.streampay.processor;


import io.aklivity.zilla.service.streampay.model.Command;
import io.aklivity.zilla.service.streampay.model.Event;
import io.aklivity.zilla.service.streampay.model.PayCommand;
import io.aklivity.zilla.service.streampay.model.PaymentRequest;
import io.aklivity.zilla.service.streampay.model.RequestCommand;
import io.aklivity.zilla.service.streampay.model.Transaction;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.logging.log4j.util.Strings;

import java.util.Map;
import java.util.UUID;


public class ProcessValidCommandSupplier implements ProcessorSupplier<String, Command, String, Command>
{

    private final String balanceStoreName;
    private final String replyTo;
    private final String transactionName;
    private final String paymentRequestsName;

    public ProcessValidCommandSupplier(
        String balanceStoreName,
        String replyTo,
        String transactionName,
        String paymentRequestsName)
    {
        this.balanceStoreName = balanceStoreName;
        this.transactionName = transactionName;
        this.paymentRequestsName = paymentRequestsName;
        this.replyTo = replyTo;
    }

    @Override
    public Processor<String, Command, String, Command> get()
    {
        return new ProcessValidCommand();
    }

    class ProcessValidCommand implements Processor<String, Command, String, Command>
    {
        ProcessorContext context;

        KeyValueStore<String, Double> balanceStore;

        @FunctionalInterface
        interface CommandProcessor
        {
            void process(Record<String, Command> record);
        }

        final Map<Class<?>, CommandProcessor> PROCESSORS =
        Map.of(
            PayCommand.class, this::processPayCommand,
            RequestCommand.class, this::processPaymentRequestCommand
        );

        @Override
        public void init(
            final ProcessorContext context)
        {
            this.context = context;
            this.balanceStore = context.getStateStore(balanceStoreName);
        }

        @Override
        public void process(
            Record<String, Command> record)
        {
            final Command command = record.value();
            final Headers headers = record.headers();
            final Header idempotencyKey = headers.lastHeader("idempotency-key");

            if (idempotencyKey == null)
            {
                processInvalidCommand(record, "Missing idempotency-key header");
            }
            else
            {
                final CommandProcessor commandProcessor = PROCESSORS.get(command.getClass());
                if (commandProcessor != null)
                {
                    commandProcessor.process(record);
                }
                else
                {
                    processInvalidCommand(record, "Unsupported command");
                }
            }
        }

        private void processPayCommand(
            Record<String, Command> record)
        {
            final PayCommand payCommand = (PayCommand) record.value();
            final Headers headers = record.headers();
            final Header userId = headers.lastHeader("userId");
            final Header correlationId = headers.lastHeader("zilla:correlation-id");
            final Headers newResponseHeaders = new RecordHeaders();
            newResponseHeaders.add(correlationId);

            final String userIdValue = new String(userId.value());
            if (validateTransaction(userIdValue, payCommand.getAmount()))
            {
                final Record<String, Transaction> withdrawalsTransaction = new Record<>(userIdValue,
                    Transaction.builder()
                        .id(UUID.randomUUID())
                        .amount(-payCommand.getAmount())
                        .timestamp(record.timestamp())
                        .build(),
                    record.timestamp());

                context.forward(withdrawalsTransaction, transactionName);

                final Record<String, Transaction> depositTransaction = new Record<>(payCommand.getUserId(),
                    Transaction.builder()
                        .id(UUID.randomUUID())
                        .amount(payCommand.getAmount())
                        .timestamp(record.timestamp())
                        .build(),
                    record.timestamp());

                context.forward(depositTransaction, transactionName);

                newResponseHeaders.add(":status", "200".getBytes());
                final Record reply = record.withHeaders(newResponseHeaders).withValue(Strings.EMPTY);
                context.forward(reply, replyTo);
            }
            else
            {
                processInvalidCommand(record, "Transaction Failed");
            }
        }

        private void processPaymentRequestCommand(
            Record<String, Command> record)
        {
            final RequestCommand requestCommand = (RequestCommand) record.value();
            final Headers headers = record.headers();
            final Header userId = headers.lastHeader("userId");
            final Header correlationId = headers.lastHeader("zilla:correlation-id");
            final Headers newResponseHeaders = new RecordHeaders();
            newResponseHeaders.add(correlationId);
            final Headers paymentRequestsRecordHeaders = new RecordHeaders();;
            paymentRequestsRecordHeaders.add(new RecordHeader("content-type", "application/json".getBytes()));
            paymentRequestsRecordHeaders.add(new RecordHeader("userId", requestCommand.getUserId().getBytes()));

            newResponseHeaders.add(":status", "200".getBytes());
            final Record reply = record.withHeaders(newResponseHeaders).withValue(Strings.EMPTY);
            context.forward(reply, replyTo);

            final Record paymentRequest = new Record(UUID.randomUUID(),
                PaymentRequest.builder()
                    .userId(new String(userId.value()))
                    .amount(requestCommand.getAmount())
                    .notes(requestCommand.getNotes())
                    .build(),
                record.timestamp(),
                paymentRequestsRecordHeaders);
            context.forward(paymentRequest, paymentRequestsName);
        }

        private void processInvalidCommand(
            Record<String, Command> record,
            String message)
        {
            final Headers headers = record.headers();
            final Header correlationId = headers.lastHeader("zilla:correlation-id");
            final Headers newResponseHeaders = new RecordHeaders();
            newResponseHeaders.add(correlationId);

            newResponseHeaders.add(":status", "400".getBytes());
            final Record reply = record
                .withHeaders(newResponseHeaders)
                .withValue(message);
            context.forward(reply, replyTo);
        }

        private boolean validateTransaction(
            String userId,
            double amount)
        {
            Double currentBalance = balanceStore.get(userId);
            currentBalance = currentBalance != null ? currentBalance : 0;
            return currentBalance - amount >= 0;
        }
    }
}
