/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.zilla.service.streampay.processor;

import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

import io.aklivity.zilla.service.streampay.model.Transaction;

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
        private KeyValueStore<String, Double> balanceStore;

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
            final String userId = record.key();
            final Record<String, Double> newBalance = new Record<>(userId,
                Double.sum(balanceStore.get(userId), record.value().getAmount()), record.timestamp());
            context.forward(newBalance, balanceName);
        }
    }
}
