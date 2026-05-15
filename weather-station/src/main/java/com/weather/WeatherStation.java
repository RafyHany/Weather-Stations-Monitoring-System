package com.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.*;

public class WeatherStation {

    /** The Kafka topic all weather stations publish to. */
    public static final String TOPIC = "weather";

    public static void main(String[] args) throws Exception {

        long   stationId        = Long.parseLong(
                System.getenv().getOrDefault("STATION_ID", "1"));
        String bootstrapServers = System.getenv().getOrDefault(
                "KAFKA_BOOTSTRAP", "localhost:9092");

        System.out.printf("=== Weather Station %d starting ===%n", stationId);
        System.out.printf("Connecting to Kafka at: %s%n", bootstrapServers);

        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        props.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        props.setProperty(ProducerConfig.RETRIES_CONFIG, "3");

        ObjectMapper mapper = new ObjectMapper();
        Random       random = new Random();

        long sNo = 1;
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {

            while (true) {

                if (random.nextDouble() < 0.10) {
                    System.out.printf("[Station %d] DROP    s_no=%-4d (simulated packet loss)%n",
                            stationId, sNo);
                    sNo++;
                    Thread.sleep(1000);
                    continue;
                }

                double batteryRoll = random.nextDouble();
                String batteryStatus;
                if      (batteryRoll < 0.30) batteryStatus = "low";
                else if (batteryRoll < 0.70) batteryStatus = "medium";
                else                         batteryStatus = "high";

                int humidity    = random.nextInt(101);
                int temperature = 60 + random.nextInt(61);
                int windSpeed   = random.nextInt(51);

                Map<String, Object> weather = new LinkedHashMap<>();
                weather.put("humidity",    humidity);
                weather.put("temperature", temperature);
                weather.put("wind_speed",  windSpeed);

                Map<String, Object> message = new LinkedHashMap<>();
                message.put("station_id",       stationId);
                message.put("s_no",             sNo);
                message.put("battery_status",   batteryStatus);
                message.put("status_timestamp", System.currentTimeMillis() / 1000L); // Unix epoch seconds
                message.put("weather",          weather);

                String json = mapper.writeValueAsString(message);

                ProducerRecord<String, String> record =
                        new ProducerRecord<>(TOPIC, String.valueOf(stationId), json);

                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        System.err.printf("[Station %d] ERROR sending: %s%n",
                                stationId, exception.getMessage());
                    }
                });

                System.out.printf("[Station %d] SENT    s_no=%-4d battery=%-6s humidity=%3d%n",
                        stationId, sNo, batteryStatus, humidity);

                sNo++;
                Thread.sleep(1000);
            }
        }
    }
}
