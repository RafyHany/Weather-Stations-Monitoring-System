package com.example.Centeral_Station.messaging;

import com.example.Centeral_Station.dto.WeatherStatus;
import com.example.Centeral_Station.service.WeatherProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Component
public class CentralStationConsumerImpl implements CentralStationConsumer, CommandLineRunner {

    @Value("${kafka.topic:weather_statuses}")
    private String topicName;

    @Value("${kafka.invalid-topic:invalid_weather_statuses}")
    private String invalidTopic;

    @Value("${kafka.dlq-topic:dlq_weather_statuses}")
    private String dlqTopic;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WeatherProcessingService weatherService;

    private final Properties consumerProps;
    private final KafkaProducer<String, String> errorProducer;

    public CentralStationConsumerImpl(WeatherProcessingService weatherService,
                                      Properties consumerProperties,
                                      KafkaProducer<String, String> deadLetterProducer) {
        this.weatherService = weatherService;
        this.consumerProps = consumerProperties;
        this.errorProducer = deadLetterProducer;
    }

    @Override
    public void run(String @NonNull ... args) {
        System.out.println("Spring Boot initialized. Spawning Kafka Consumer thread...");
        Thread consumerThread = new Thread(this::startConsuming);
        consumerThread.setName("weather-station-kafka-consumer");
        consumerThread.start();
    }

    @Override
    public void startConsuming() {
        System.out.println("Starting consumer polling loop...");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList(topicName));

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    WeatherStatus status;

                    try {
                        status = objectMapper.readValue(record.value(), WeatherStatus.class);
                    } catch (Exception e) {
                        routeToErrorChannel(invalidTopic, record, e);
                        continue;
                    }

                    try {
                        weatherService.processStatus(status);
                    } catch (Exception e) {
                        routeToErrorChannel(dlqTopic, record, e);
                    }
                }
            }
        }
    }

    private void routeToErrorChannel(String targetTopic, ConsumerRecord<String, String> failedRecord, Exception exception) {
        ProducerRecord<String, String> errorRecord = new ProducerRecord<>(
                targetTopic,
                failedRecord.key(),
                failedRecord.value()
        );

        String errorMessage = exception.getMessage() != null ? exception.getMessage() : "Unknown exception occurred";
        String errorType = exception.getClass().getSimpleName();

        errorRecord.headers().add("error.message", errorMessage.getBytes(StandardCharsets.UTF_8));
        errorRecord.headers().add("error.type", errorType.getBytes(StandardCharsets.UTF_8));

        errorProducer.send(errorRecord);
    }
}