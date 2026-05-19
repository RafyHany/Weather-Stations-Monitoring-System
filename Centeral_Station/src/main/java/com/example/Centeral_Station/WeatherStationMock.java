package com.example.Centeral_Station;

import com.example.Centeral_Station.dto.WeatherStatus;
import com.example.Centeral_Station.enums.BatteryStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.util.Properties;
import java.util.Random;

public class WeatherStationMock {

    // FIX 1: Ensure this matches the exact topic your Central Station is listening to
    private static final String TOPIC_NAME = "weather";

    // Assumes you are running this straight from IntelliJ/Eclipse on your Ubuntu host
    private static final String BOOTSTRAP_SERVERS = "127.0.0.1:9092";

    // FIX 2: Send enough to cleanly trigger your 10,000 threshold
    private static final int MESSAGES_TO_SEND = 10500;

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        ObjectMapper objectMapper = new ObjectMapper();
        Random random = new Random();

        System.out.println("Starting Weather Station Flood Mock...");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            int messagesSent = 0;

            for (long i = 1; i <= MESSAGES_TO_SEND; i++) {
                // Generate random station ID (from 1 to 10)
                long stationId = random.nextInt(10) + 1;

                // Generate battery status
                double batteryRoll = random.nextDouble();
                BatteryStatus batteryStatus;
                if (batteryRoll < 0.30) {
                    batteryStatus = BatteryStatus.LOW;
                } else if (batteryRoll < 0.70) {
                    batteryStatus = BatteryStatus.MEDIUM;
                } else {
                    batteryStatus = BatteryStatus.HIGH;
                }

                // Generate random weather metrics
                byte humidity = (byte) random.nextInt(101); // 0 to 100
                int temperature = random.nextInt(120); // 0 to 119 F
                short windSpeed = (short) random.nextInt(50); // 0 to 49 km/h

                // Construct the DTO
                WeatherStatus.Weather weather = new WeatherStatus.Weather(humidity, temperature, windSpeed);
                WeatherStatus status = new WeatherStatus(
                        stationId,
                        i, // Sequence number
                        batteryStatus,
                        Instant.now().getEpochSecond(), // Current Unix timestamp
                        weather
                );

                // Serialize to JSON and Send
                try {
                    String jsonMessage = objectMapper.writeValueAsString(status);
                    ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC_NAME, String.valueOf(stationId), jsonMessage);

                    producer.send(record);
                    messagesSent++;

                    // FIX 3: Removed Thread.sleep() to blast the broker instantly
                } catch (Exception e) {
                    System.err.println("Failed to send message: " + e.getMessage());
                }
            }

            // Flush ensures all messages actually leave the producer buffer before the program exits
            producer.flush();
            System.out.printf("Finished! Blasted %d messages instantly.%n", messagesSent);
        }
    }
}