/*
 * Copyright 2021-2022 Aklivity. All rights reserved.
 */
package io.aklivity.zilla.service.streampay;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.StreamsBuilderFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsConfiguration;


@Configuration
@EnableKafka
@EnableKafkaStreams
public class KafkaConfig
{
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    @Value("${spring.kafka.security-protocol:PLAINTEXT}")
    private String securityProtocol;
    @Value("${spring.kafka.application-id}")
    private String applicationId;
    @Value("${spring.kafka.streams.state.dir:#{null}}")
    private String stateDir;

    public KafkaConfig()
    {
    }

    @Bean(
        name = {"defaultKafkaStreamsConfig"}
    )
    KafkaStreamsConfiguration kafkaStreamsConfig()
    {
        Map<String, Object> props = new HashMap();
        props.put("application.id", this.applicationId);
        props.put("bootstrap.servers", this.bootstrapServers);
        props.put("security.protocol", this.securityProtocol);
        props.put("default.key.serde", Serdes.String().getClass().getName());
        props.put("default.value.serde", Serde.class.getName());
        props.put("default.deserialization.exception.handler", LogAndContinueExceptionHandler.class);
        if (this.stateDir != null)
        {
            props.put("state.dir", this.stateDir);
        }

        props.put("commit.interval.ms", 0);
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public StreamsBuilderFactoryBeanCustomizer streamsBuilderFactoryBeanCustomizer()
    {
        return sfb -> sfb.setStreamsUncaughtExceptionHandler(exception ->
                        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD);
    }
}
