package com.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;

import java.util.*;

public class RainDetector {

    public static final String INPUT_TOPIC  = "weather";
    public static final String OUTPUT_TOPIC = "rain-alerts";

    public static void main(String[] args) throws Exception {

        String bootstrapServers = System.getenv().getOrDefault(
                "KAFKA_BOOTSTRAP", "localhost:9092");

        System.out.println("=== Rain Detector starting ===");
        System.out.printf("Reading from : %s%n", INPUT_TOPIC);
        System.out.printf("Writing to   : %s%n", OUTPUT_TOPIC);
        System.out.printf("Kafka at     : %s%n", bootstrapServers);

        Properties props = new Properties();

        // APPLICATION_ID: a unique name for this consumer group.
        // Kafka uses it to remember which messages have been processed.
        // If this app restarts, it resumes from the last committed offset.
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "rain-detector-app");

        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Serde = Serializer + Deserializer.
        // Serdes.String() handles both directions for UTF-8 text (our JSON strings).
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
                Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
                Serdes.String().getClass().getName());

        ObjectMapper mapper = new ObjectMapper();
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> weatherStream =
                builder.stream(INPUT_TOPIC,
                        Consumed.with(Serdes.String(), Serdes.String()));

        KStream<String, String> rainyStream = weatherStream.filter((key, value) -> {
            try {
                // Parse the JSON string back into a Map
                Map<?, ?> msg     = mapper.readValue(value, Map.class);
                Map<?, ?> weather = (Map<?, ?>) msg.get("weather");
                int humidity = ((Number) weather.get("humidity")).intValue();
                boolean isRaining = humidity > 70;
                if (isRaining) {
                    System.out.printf("[Detector] RAIN from station %s, humidity=%d%n",
                            key, humidity);
                }
                return isRaining;
            } catch (Exception e) {
                System.err.println("[Detector] Skipping malformed message: " + e.getMessage());
                return false;
            }
        });

        KStream<String, String> alertStream = rainyStream.mapValues(value -> {
            try {
                Map<?, ?> msg     = mapper.readValue(value, Map.class);
                Map<?, ?> weather = (Map<?, ?>) msg.get("weather");

                Map<String, Object> alert = new LinkedHashMap<>();
                alert.put("station_id",       msg.get("station_id"));
                alert.put("s_no",             msg.get("s_no"));
                alert.put("status_timestamp", msg.get("status_timestamp"));
                alert.put("alert_type",       "RAIN_DETECTED");
                alert.put("humidity",         weather.get("humidity"));

                return mapper.writeValueAsString(alert);
            } catch (Exception e) {
                System.err.println("[Detector] Transform error: " + e.getMessage());
                return value;
            }
        });

        alertStream.to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.String()));
        KafkaStreams streams = new KafkaStreams(builder.build(), props);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Detector] Shutdown signal received, closing...");
            streams.close();
        }));

        streams.start();
        System.out.println("[Detector] Running. Waiting for weather messages...");
        Thread.currentThread().join();
    }
}
