/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.zilla.demo.streampay.stream.processor;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.logging.log4j.util.Strings;

import io.aklivity.zilla.demo.streampay.data.model.Command;

public class RejectInvalidCommandSupplier implements ProcessorSupplier<String, Command, String, Command>
{
    private final String replyTo;

    public RejectInvalidCommandSupplier(
        String replyTo)
    {
        this.replyTo = replyTo;
    }

    @Override
    public Processor<String, Command, String, Command> get()
    {
        return new Processor<>()
        {
            ProcessorContext context;

            @Override
            public void init(
                final ProcessorContext context)
            {
                this.context = context;
            }

            @Override
            public void process(
                Record<String, Command> record)
            {
                final Headers headers = record.headers();
                final Header correlationId = headers.lastHeader("zilla:correlation-id");
                final Headers newHeaders = new RecordHeaders();
                newHeaders.add(correlationId);
                newHeaders.add(":status", "412".getBytes());
                final Record newRecord = record.withHeaders(newHeaders).withValue(Strings.EMPTY);
                context.forward(newRecord, replyTo);
            }
        };
    }
}
