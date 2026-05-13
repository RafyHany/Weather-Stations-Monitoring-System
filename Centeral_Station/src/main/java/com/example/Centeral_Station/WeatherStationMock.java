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

    private static final String TOPIC_NAME = "weather_statuses";
    private static final String BOOTSTRAP_SERVERS = "127.0.0.1:9092";
    private static final int MESSAGES_TO_SEND = 15000; // Adjust this to 10000+ later to test Parquet batching!

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        ObjectMapper objectMapper = new ObjectMapper();
        Random random = new Random();

        System.out.println("Starting Weather Station Mock Producer...");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {

            int messagesSent = 0;
            int messagesDropped = 0;

            for (long i = 1; i <= MESSAGES_TO_SEND; i++) {
                // 1. Simulate 10% message drop rate
                if (random.nextDouble() < 0.10) {
                    messagesDropped++;
                    continue; // Skip sending this message
                }

                // 2. Generate random station ID (from 1 to 10)
                long stationId = random.nextInt(10) + 1;

                // 3. Generate battery status (30% Low, 40% Medium, 30% High)
                double batteryRoll = random.nextDouble();
                BatteryStatus batteryStatus;
                if (batteryRoll < 0.30) {
                    batteryStatus = BatteryStatus.LOW;
                } else if (batteryRoll < 0.70) { // 0.30 to 0.70 is a 40% spread
                    batteryStatus = BatteryStatus.MEDIUM;
                } else {
                    batteryStatus = BatteryStatus.HIGH;
                }

                // 4. Generate random weather metrics
                byte humidity = (byte) random.nextInt(101); // 0 to 100
                int temperature = random.nextInt(120); // 0 to 119 F
                short windSpeed = (short) random.nextInt(50); // 0 to 49 km/h

                // 5. Construct the DTO
                WeatherStatus.Weather weather = new WeatherStatus.Weather(humidity, temperature, windSpeed);
                WeatherStatus status = new WeatherStatus(
                        stationId,
                        i, // Sequence number
                        batteryStatus,
                        Instant.now().getEpochSecond(), // Current Unix timestamp
                        weather
                );

                // 6. Serialize to JSON and Send
                try {
                    String jsonMessage = objectMapper.writeValueAsString(status);
                    ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC_NAME, String.valueOf(stationId), jsonMessage);

                    producer.send(record);
                    messagesSent++;

                    // Optional: Sleep for a tiny fraction of a second so you can watch the console
                    Thread.sleep(2);

                } catch (Exception e) {
                    System.err.println("Failed to send message: " + e.getMessage());
                }
            }

            System.out.printf("Finished! Sent %d messages. Dropped %d messages.%n", messagesSent, messagesDropped);
        }
    }
}