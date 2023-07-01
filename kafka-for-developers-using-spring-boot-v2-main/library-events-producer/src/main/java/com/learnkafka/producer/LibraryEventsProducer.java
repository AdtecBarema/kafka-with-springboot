package com.learnkafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.domain.LibraryEvent;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class LibraryEventsProducer {

    @Value("${spring.kafka.topic}")
    public String topic;

    private final KafkaTemplate<Integer, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    public LibraryEventsProducer(KafkaTemplate<Integer, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }


    public CompletableFuture<SendResult<Integer, String>> sendLibraryEvent(LibraryEvent libraryEvent) throws JsonProcessingException {
        Integer key = libraryEvent.libraryEventId();
        String value = objectMapper.writeValueAsString(libraryEvent);

        //1. blocking call - get metadata about the kafka cluster
        //2. Send message happens - Returns a CompletableFuture

        CompletableFuture<SendResult<Integer, String>> completableFuture = kafkaTemplate.send(topic, key, value);
        return completableFuture
                .whenComplete((sendResult, throwable) -> {
                    if (throwable != null) {
                        handleFailure(key, value, throwable);
                    } else {
                        handleSuccess(key, value, sendResult);
                    }
                });
    }


    public SendResult<Integer, String> sendLibraryEvent_approach2(LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException {
        Integer key = libraryEvent.libraryEventId();
        String value = objectMapper.writeValueAsString(libraryEvent);

        //1. blocking call - get metadata about the kafka cluster
        //2. Block and wait until the message is sent to the kafka cluster

        SendResult<Integer, String> sendResult = kafkaTemplate.send(topic, key, value).get();
        handleSuccess(key, value, sendResult);
        return sendResult;
    }

    //Here in this method instead of sending  key and value directly, we can create a wrapper object called "ProducerRecord", which is a composite of these three objects
    public CompletableFuture<SendResult<Integer, String>> sendLibraryEvent_approach3(LibraryEvent libraryEvent) throws JsonProcessingException {
        Integer key = libraryEvent.libraryEventId();
        String value = objectMapper.writeValueAsString(libraryEvent);

        ProducerRecord<Integer,String> producerRecord= buildProducerRecord(key, value,topic);

        //1. blocking call - get metadata about the kafka cluster
        //2. Send message happens - Returns a CompletableFuture

        CompletableFuture<SendResult<Integer, String>> completableFuture = kafkaTemplate.send(producerRecord);
        return completableFuture
                .whenComplete((sendResult, throwable) -> {
                    if (throwable != null) {
                        handleFailure(key, value, throwable);
                    } else {
                        handleSuccess(key, value, sendResult);
                    }
                });
    }

    private ProducerRecord<Integer, String> buildProducerRecord(Integer key, String value,  String topic) {

        List<Header> recordHeaders = List.of(new RecordHeader("event-source", "scanner".getBytes()));

        return new ProducerRecord<>(topic, null, key, value, recordHeaders);
    }


    private void handleSuccess(Integer key, String value, SendResult<Integer, String> sendResult) {
        log.info("Message Sent Successfully for the key: {} and the value :{}, partition is {}", key, value, sendResult.getRecordMetadata().partition());
    }

    private void handleFailure(Integer key, String value, Throwable throwable) {
        log.error("Error sending the message and the exception is {}", throwable.getMessage(), throwable);
    }

}