package com.learnkafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;

public class LibraryEventsProducerBuilder {
    private KafkaTemplate<Integer, String> kafkaTemplate;
    private ObjectMapper objectMapper;

    public LibraryEventsProducerBuilder setKafkaTemplate(KafkaTemplate<Integer, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        return this;
    }

    public LibraryEventsProducerBuilder setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public LibraryEventsProducer createLibraryEventsProducer() {
        return new LibraryEventsProducer(kafkaTemplate, objectMapper);
    }
}