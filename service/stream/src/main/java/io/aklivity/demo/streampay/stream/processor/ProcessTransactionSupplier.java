/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.demo.streampay.stream.processor;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

import io.aklivity.demo.streampay.data.model.Balance;
import io.aklivity.demo.streampay.data.model.Transaction;


public class ProcessTransactionSupplier implements ProcessorSupplier<String, Transaction, String, Transaction>
{
    private String balanceStoreName;
    private String balanceName;

    public ProcessTransactionSupplier(
        String balanceStoreName,
        String balanceName)
    {
        this.balanceStoreName = balanceStoreName;
        this.balanceName = balanceName;
    }

    @Override
    public Processor<String, Transaction, String, Transaction> get()
    {
        return new AggregateBalance();
    }

    class AggregateBalance implements Processor<String, Transaction, String, Transaction>
    {
        private ProcessorContext context;
        private KeyValueStore<String, Balance> balanceStore;

        @Override
        public void init(
            final ProcessorContext context)
        {
            this.context = context;
            this.balanceStore = context.getStateStore(balanceStoreName);
        }

        @Override
        public void process(
            Record<String, Transaction> record)
        {
            final Headers balanceRecordHeaders = new RecordHeaders();
            balanceRecordHeaders.add(new RecordHeader("content-type", "application/json".getBytes()));
            final String userId = record.key();
            double currentBalance = balanceStore.get(userId) == null ? 0 : balanceStore.get(userId).getBalance();
            final double newBalanceValue = Double.sum(currentBalance, record.value().getAmount());
            final Balance newBalance = Balance.builder().balance(newBalanceValue).timestamp(record.timestamp()).build();
            final Record<String, Balance> newBalanceRecord = new Record<>(userId,
                newBalance, record.timestamp(), balanceRecordHeaders);
            context.forward(newBalanceRecord, balanceName);
            balanceStore.put(userId, newBalance);
        }
    }
}
