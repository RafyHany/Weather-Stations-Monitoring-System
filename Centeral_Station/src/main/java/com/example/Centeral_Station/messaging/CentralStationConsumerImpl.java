package com.example.Centeral_Station.messaging;

import com.example.Centeral_Station.dto.WeatherStatus;
import com.example.Centeral_Station.service.WeatherProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Component
public class CentralStationConsumerImpl implements CentralStationConsumer, CommandLineRunner {
    @Value("${kafka.topic:weather_statuses}")
    private String topicName;

    @Value("${kafka.bootstrap-servers:127.0.0.1:9092}")
    private String bootstrapServers;

    @Value("${kafka.group-id:central-station-group}")
    private String groupId;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WeatherProcessingService weatherService;

    public CentralStationConsumerImpl(WeatherProcessingService weatherService) {
        this.weatherService = weatherService;
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
        Properties properties = getProperties();

        System.out.printf("Starting consumer connecting to Kafka at %s...%n", bootstrapServers);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(Collections.singletonList(topicName));

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        WeatherStatus status = objectMapper.readValue(record.value(), WeatherStatus.class);
                        weatherService.processStatus(status);
                    } catch (Exception e) {
                        weatherService.handleFailedMessage(record.value(), e);
                    }
                }
            }
        }
    }

    private Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return properties;
    }
}